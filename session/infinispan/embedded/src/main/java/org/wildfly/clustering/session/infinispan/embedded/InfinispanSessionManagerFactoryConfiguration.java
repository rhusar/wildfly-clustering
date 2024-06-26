/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session.infinispan.embedded;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * @author Paul Ferraro
 */
public interface InfinispanSessionManagerFactoryConfiguration extends EmbeddedCacheConfiguration {

	CacheContainerCommandDispatcherFactory getCommandDispatcherFactory();
}
