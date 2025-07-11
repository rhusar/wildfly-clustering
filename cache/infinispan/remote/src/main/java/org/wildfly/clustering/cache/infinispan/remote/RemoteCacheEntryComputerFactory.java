/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.cache.CacheEntryMutatorFactory;

/**
 * Factory that creates compute-based Mutator instances.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @param <O> the function operand type
 */
public class RemoteCacheEntryComputerFactory<K, V, O> implements CacheEntryMutatorFactory<K, O> {

	private final RemoteCache<K, V> cache;
	private final Function<O, BiFunction<Object, V, V>> functionFactory;

	RemoteCacheEntryComputerFactory(RemoteCache<K, V> cache, Function<O, BiFunction<Object, V, V>> functionFactory) {
		this.cache = cache;
		this.functionFactory = functionFactory;
	}

	@Override
	public CacheEntryMutator createMutator(K key, O operand) {
		return new RemoteCacheEntryComputer<>(this.cache, key, this.functionFactory.apply(operand));
	}
}
