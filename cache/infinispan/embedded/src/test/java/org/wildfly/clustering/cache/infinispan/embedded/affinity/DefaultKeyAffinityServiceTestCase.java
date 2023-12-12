/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.affinity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import org.infinispan.AdvancedCache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.KeyDistribution;

/**
 * Unit test for {@link DefaultKeyAffinityService}.
 * @author Paul Ferraro
 */
public class DefaultKeyAffinityServiceTestCase {

	private static final int SEGMENTS = 3;
	private static final int LOCAL_SEGMENT = 0;
	private static final int REMOTE_SEGMENT = 1;
	private static final int FILTERED_SEGMENT = 2;

	@Test
	public void test() {
		ExecutorService executor = Executors.newCachedThreadPool();
		try {
			KeyGenerator<UUID> generator = mock(KeyGenerator.class);
			AdvancedCache<UUID, Object> cache = mock(AdvancedCache.class);
			KeyDistribution distribution = mock(KeyDistribution.class);
			ConsistentHash hash = mock(ConsistentHash.class);
			Address local = mock(Address.class);
			Address remote = mock(Address.class);
			Address standby = mock(Address.class);
			Address ignored = mock(Address.class);
			KeyAffinityService<UUID> service = new DefaultKeyAffinityService<>(cache, generator, address -> (address != ignored), executor, c -> hash, (c, h) -> distribution);

			List<Address> members = List.of(local, remote, ignored, standby);

			when(hash.getMembers()).thenReturn(members);
			when(hash.getPrimarySegmentsForOwner(local)).thenReturn(Set.of(LOCAL_SEGMENT));
			when(hash.getPrimarySegmentsForOwner(remote)).thenReturn(Set.of(REMOTE_SEGMENT));
			when(hash.getPrimarySegmentsForOwner(standby)).thenReturn(Set.of());
			when(hash.getPrimarySegmentsForOwner(ignored)).thenReturn(Set.of(FILTERED_SEGMENT));

			// Mock a sufficient number of keys
			int[] keysPerSegment = new int[3];
			Arrays.fill(keysPerSegment, 0);
			int minKeysPerSegment = DefaultKeyAffinityService.DEFAULT_QUEUE_SIZE * SEGMENTS;
			IntPredicate needMoreKeys = keys -> (keys < minKeysPerSegment);
			OngoingStubbing<UUID> stub = when(generator.getKey());
			while (IntStream.of(keysPerSegment).anyMatch(needMoreKeys)) {
				UUID key = UUID.randomUUID();
				int segment = getSegment(key);
				keysPerSegment[segment] += 1;

				stub = stub.thenReturn(key);

				when(distribution.getPrimaryOwner(key)).thenReturn(members.get(segment));
			}

			assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(local));
			assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(remote));
			assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(standby));
			// This should throw IAE, since address does not pass filter
			assertThrows(IllegalArgumentException.class, () -> service.getKeyForAddress(ignored));

			service.start();

			try {
				int iterations = DefaultKeyAffinityService.DEFAULT_QUEUE_SIZE / 2;
				for (int i = 0; i < iterations; ++i) {
					UUID key = service.getKeyForAddress(local);
					int segment = getSegment(key);
					assertEquals(LOCAL_SEGMENT, segment);

					key = service.getCollocatedKey(key);
					segment = getSegment(key);
					assertEquals(LOCAL_SEGMENT, segment);

					key = service.getKeyForAddress(remote);
					segment = getSegment(key);
					assertEquals(REMOTE_SEGMENT, segment);

					key = service.getCollocatedKey(key);
					segment = getSegment(key);
					assertEquals(REMOTE_SEGMENT, segment);
				}

				// This should return a random key
				assertNotNull(service.getKeyForAddress(standby));
				// This should throw IAE, since address does not pass filter
				assertThrows(IllegalArgumentException.class, () -> service.getKeyForAddress(ignored));
			} finally {
				service.stop();
			}
		} finally {
			executor.shutdown();
		}
	}

	private static int getSegment(UUID key) {
		return Math.abs(key.hashCode()) % SEGMENTS;
	}
}
