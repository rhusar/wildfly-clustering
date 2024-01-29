/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.cache;

import java.util.Map;
import java.util.function.Supplier;

import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.cache.attributes.SessionAttributes;
import org.wildfly.clustering.session.cache.attributes.SessionAttributesFactory;
import org.wildfly.clustering.session.cache.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.session.cache.metadata.SessionMetaDataFactory;

/**
 * @param <DC> the deployment context type
 * @param <MV> the session metadata type
 * @param <AV> the session attributes type
 * @param <SC> the session context type
 * @author Paul Ferraro
 */
public class CompositeSessionFactory<DC, MV extends Contextual<SC>, AV, SC> extends CompositeImmutableSessionFactory<MV, AV> implements SessionFactory<DC, MV, AV, SC> {
	private final SessionMetaDataFactory<MV> metaDataFactory;
	private final SessionAttributesFactory<DC, AV> attributesFactory;
	private final Supplier<SC> contextFactory;

	public CompositeSessionFactory(SessionMetaDataFactory<MV> metaDataFactory, SessionAttributesFactory<DC, AV> attributesFactory, Supplier<SC> localContextFactory) {
		super(metaDataFactory, attributesFactory);
		this.metaDataFactory = metaDataFactory;
		this.attributesFactory = attributesFactory;
		this.contextFactory = localContextFactory;
	}

	@Override
	public SessionMetaDataFactory<MV> getMetaDataFactory() {
		return this.metaDataFactory;
	}

	@Override
	public SessionAttributesFactory<DC, AV> getAttributesFactory() {
		return this.attributesFactory;
	}

	@Override
	public Session<SC> createSession(String id, Map.Entry<MV, AV> entry, DC context) {
		MV metaDataValue = entry.getKey();
		AV attributesValue = entry.getValue();
		if ((metaDataValue == null) || (attributesValue == null)) return null;
		InvalidatableSessionMetaData metaData = this.metaDataFactory.createSessionMetaData(id, metaDataValue);
		SessionAttributes attributes = this.attributesFactory.createSessionAttributes(id, attributesValue, metaData, context);
		return new CompositeSession<>(id, metaData, attributes, metaDataValue.getContext(), this.contextFactory, this);
	}
}
