/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.clustering.context.Context;
import org.wildfly.common.function.Functions;

/**
 * Marshals an object to and from a {@link ByteBuffer}.
 * @author Paul Ferraro
 */
public interface ByteBufferMarshaller extends Marshaller<Object, ByteBuffer> {
	Logger LOGGER = Logger.getLogger(ByteBufferMarshaller.class);

	/**
	 * Reads an object from the specified input stream.
	 * @param input an input stream
	 * @return the unmarshalled object
	 * @throws IOException if the object could not be read
	 */
	Object readFrom(InputStream input) throws IOException;

	/**
	 * Writes the specified object to the specified output stream.
	 * @param output an output stream
	 * @param object an object to marshal
	 * @throws IOException if the object could not be written
	 */
	void writeTo(OutputStream output, Object object) throws IOException;

	@Override
	default Object read(ByteBuffer buffer) throws IOException {
		try (Context context = this.getContextProvider().get()) {
			try (InputStream input = new ByteBufferInputStream(buffer)) {
				return this.readFrom(input);
			}
		}
	}

	@Override
	default ByteBuffer write(Object object) throws IOException {
		try (Context context = this.getContextProvider().get()) {
			OptionalInt size = this.size(object);
			try (ByteBufferOutputStream output = new ByteBufferOutputStream(size)) {
				this.writeTo(output, object);
				ByteBuffer buffer = output.getBuffer();
				if (size.isPresent()) {
					int predictedSize = size.getAsInt();
					int actualSize = buffer.limit() - buffer.arrayOffset();
					if (predictedSize < actualSize) {
						LOGGER.debugf("Buffer size prediction too small for %s (%s), predicted = %d, actual = %d", object, (object != null) ? object.getClass().getCanonicalName() : null, predictedSize, actualSize);
					}
				} else {
					LOGGER.tracef("Buffer size prediction missing for %s (%s)", object, (object != null) ? object.getClass().getCanonicalName() : null);
				}
				return buffer;
			}
		}
	}

	/**
	 * Returns the marshalled size of the specified object.
	 * @param object the object whose marshalled size is to be computed
	 * @return the marshalled size of the specified object.
	 */
	default OptionalInt size(Object object) {
		return OptionalInt.empty();
	}

	/**
	 * Returns a provider of context to use during read/write operations.
	 * @return a context provider
	 */
	default Supplier<Context> getContextProvider() {
		return Functions.constantSupplier(Context.EMPTY);
	}
}
