/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.infinispan.remote;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.util.Version;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Paul Ferraro
 */
public class InfinispanServerContainer extends GenericContainer<InfinispanServerContainer> implements Function<ConfigurationBuilder, RemoteCacheContainer>, Consumer<OutputFrame> {

	static final Logger LOGGER = Logger.getLogger(InfinispanServerContainer.class);

	static final DockerImageName DOCKER_IMAGE = DockerImageName.parse(System.getProperty("infinispan.server.image", "quay.io/infinispan/server:" + Version.getMajorMinor()));
	static final int HOTROD_PORT = Integer.parseInt(System.getProperty("infinispan.server.port", "11222"));
	static final String USERNAME = System.getProperty("infinispan.server.username", "admin");
	static final String PASSWORD = System.getProperty("infinispan.server.password", "changeme");

	static final String NETWORK_MODE = System.getProperty("docker.network.mode", "bridge");
	static final String HOST_NETWORK_MODE = "host";

	InfinispanServerContainer() {
		super(DOCKER_IMAGE);

		this.setNetworkMode(NETWORK_MODE);
		if (!this.getNetworkMode().equals(HOST_NETWORK_MODE)) {
			this.setExposedPorts(java.util.List.of(HOTROD_PORT));
		}
		this.setHostAccessible(true);
		this.withLogConsumer(this);
		// Wait for server started log message
		this.setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*\\QISPN080001\\E.*").withTimes(1).withStartupTimeout(Duration.ofMinutes(2)));
		this.withEnv("USER", USERNAME).withEnv("PASS", PASSWORD);
	}

	@Override
	public void start() {
		LOGGER.infof("Starting Infinispan server container: %s", this.getDockerImageName());
		super.start();
	}

	@Override
	public RemoteCacheContainer apply(ConfigurationBuilder builder) {
		String host = this.getHost();
		int port = !this.getNetworkMode().equals(HOST_NETWORK_MODE) ? this.getMappedPort(HOTROD_PORT) : HOTROD_PORT;
		LOGGER.infof("Creating HotRod client connecting to %s:%d", host, port);
		builder.addServer().host(host).port(port).security().authentication()
			.username(this.getEnvMap().get("USER"))
			.password(this.getEnvMap().get("PASS"))
			;
		return new RemoteCacheManager(builder.build(), false);
	}

	@Override
	public void accept(OutputFrame frame) {
		OutputFrame.OutputType type = frame.getType();
		if (type != OutputType.END) {
			String message = frame.getUtf8String().replaceAll("((\\r?\\n)|(\\r))$", "");
			LOGGER.logf(type == OutputType.STDERR ? Level.ERROR : Level.INFO, message);
		}
	}
}