/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.cache;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.Batcher;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManagerConfiguration;

/**
 * A {@link SessionManagerConfiguration} implementation that delegates to another {@link SessionManagerConfiguration}.
 * @author Paul Ferraro
 * @param <C> the servlet context type
 */
public class DelegatingSessionManagerConfiguration<C, B extends Batch> implements SessionManagerConfiguration<C, B> {

	private final SessionManagerConfiguration<C, B> configuration;

	public DelegatingSessionManagerConfiguration(SessionManagerConfiguration<C, B> configuration) {
		this.configuration = configuration;
	}

	@Override
	public Consumer<ImmutableSession> getExpirationListener() {
		return this.configuration.getExpirationListener();
	}

	@Override
	public Duration getTimeout() {
		return this.configuration.getTimeout();
	}

	@Override
	public C getContext() {
		return this.configuration.getContext();
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		return this.configuration.getIdentifierFactory();
	}

	@Override
	public CacheProperties getCacheProperties() {
		return this.configuration.getCacheProperties();
	}

	@Override
	public Batcher<B> getBatcher() {
		return this.configuration.getBatcher();
	}
}
