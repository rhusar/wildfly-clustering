/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

/**
 * Unit tests for {@link ByteBufferMarshalledValue}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public abstract class ByteBufferMarshalledKeyFactoryTestCase extends ByteBufferMarshalledValueFactoryTestCase {

	private final ByteBufferMarshalledKeyFactory factory;

	protected ByteBufferMarshalledKeyFactoryTestCase(MarshallingTesterFactory factory) {
		this(factory.getMarshaller());
	}

	protected ByteBufferMarshalledKeyFactoryTestCase(ByteBufferMarshaller marshaller) {
		this(marshaller, new ByteBufferMarshalledKeyFactory(marshaller));
	}

	private ByteBufferMarshalledKeyFactoryTestCase(ByteBufferMarshaller marshaller, ByteBufferMarshalledKeyFactory factory) {
		super(marshaller, factory);
		this.factory = factory;
	}

	@Override
	public void testHashCode() throws Exception {
		UUID uuid = UUID.randomUUID();
		ByteBufferMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);
		assertThat(mv).hasSameHashCodeAs(uuid);

		ByteBufferMarshalledValue<UUID> copy = replicate(mv);
		assertThat(copy).hasSameHashCodeAs(uuid);

		mv = this.factory.createMarshalledValue(null);
		assertThat(mv.hashCode()).isEqualTo(0);
	}
}
