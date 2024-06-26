/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.cache.attributes.fine;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValue;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapComputeFunctionMarshallerTestCase {

	@Test
	public void test() {
		ProtoStreamTesterFactory factory = new ProtoStreamTesterFactory(new FineSessionAttributesSerializationContextInitializer());
		ByteBufferMarshaller marshaller = factory.getMarshaller();
		Map<String, ByteBufferMarshalledValue<UUID>> map = new TreeMap<>();
		map.put("foo", new ByteBufferMarshalledValue<>(UUID.randomUUID(), marshaller));
		map.put("bar", new ByteBufferMarshalledValue<>(UUID.randomUUID(), marshaller));
		Consumer<SessionAttributeMapComputeFunction<ByteBufferMarshalledValue<UUID>>> tester = factory.createTester();
		tester.accept(new SessionAttributeMapComputeFunction<>(map));
	}
}
