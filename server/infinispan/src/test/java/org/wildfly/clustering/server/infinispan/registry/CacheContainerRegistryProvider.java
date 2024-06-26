/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.registry;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.wildfly.clustering.server.AutoCloseableProvider;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupProvider;
import org.wildfly.clustering.server.infinispan.EmbeddedCacheManagerGroupProvider;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;

/**
 * Provides a {@link Registry} to an integration test.
 * @author Paul Ferraro
 * @param <K> the registry key type
 * @param <V> the registry value type
 */
public class CacheContainerRegistryProvider<K, V> extends AutoCloseableProvider implements Function<Map.Entry<K, V>, Registry<CacheContainerGroupMember, K, V>> {
	private static final String CACHE_NAME = "registry";

	private final RegistryFactory<CacheContainerGroupMember, K, V> factory;

	@SuppressWarnings("resource")
	public CacheContainerRegistryProvider(String clusterName, String memberName) throws Exception {
		CacheContainerGroupProvider provider = new EmbeddedCacheManagerGroupProvider(clusterName, memberName);
		this.accept(provider::close);

		EmbeddedCacheManager manager = provider.getCacheManager();
		manager.defineConfiguration(CACHE_NAME, new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build());
		this.accept(() -> manager.undefineConfiguration(CACHE_NAME));

		Cache<?, ?> cache = manager.getCache(CACHE_NAME);
		cache.start();
		this.accept(cache::stop);

		CacheRegistryConfiguration config = new CacheRegistryConfiguration() {
			@SuppressWarnings("unchecked")
			@Override
			public <KK, VV> Cache<KK, VV> getCache() {
				return (Cache<KK, VV>) cache;
			}

			@Override
			public CacheContainerGroup getGroup() {
				return provider.getGroup();
			}
		};
		this.factory = RegistryFactory.singleton(new BiFunction<>() {
			@Override
			public Registry<CacheContainerGroupMember, K, V> apply(Map.Entry<K, V> entry, Runnable closeTask) {
				return new CacheRegistry<>(config, entry, closeTask);
			}
		});
	}

	@Override
	public Registry<CacheContainerGroupMember, K, V> apply(Map.Entry<K, V> entry) {
		return this.factory.createRegistry(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
	}
}
