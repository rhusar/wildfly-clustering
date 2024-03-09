/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.container;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.arquillian.Deployment;

/**
 * @author Paul Ferraro
 */
public interface SessionManagementTesterConfiguration extends SessionManagementEndpointConfiguration {

	Class<?> getEndpointClass();

	default URI locateEndpoint(Deployment deployment) {
		return deployment.locate(this.getEndpointClass()).resolve(ENDPOINT_NAME);
	}

	default UnaryOperator<HttpClient.Builder> getHttpClientConfigurator() {
		return UnaryOperator.identity();
	}

	default boolean isTransactional() {
		return false;
	}
}