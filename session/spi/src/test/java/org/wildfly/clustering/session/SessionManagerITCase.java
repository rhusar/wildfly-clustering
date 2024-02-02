/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.container.ContainerFacadeProvider;
import org.wildfly.common.function.ExceptionBiFunction;
import org.wildfly.common.function.Functions;

/**
 * Session manager integration tests.
 * @author Paul Ferraro
 */
public abstract class SessionManagerITCase<B extends Batch, P extends SessionManagerParameters> {

	private static final String DEPLOYMENT_CONTEXT = "deployment";
	private static final Supplier<AtomicReference<String>> SESSION_CONTEXT_FACTORY = AtomicReference::new;
	private static final ContainerFacadeProvider<Map.Entry<ImmutableSession, String>, String, PassivationListener<String>> CONTAINER_FACADE_PROVIDER = new MockContainerFacadeProvider<>();

	private final ExceptionBiFunction<P, String, SessionManagerFactoryProvider<String, B>, Exception> factory;

	protected SessionManagerITCase(ExceptionBiFunction<P, String, SessionManagerFactoryProvider<String, B>, Exception> factory) {
		this.factory = factory;
	}

	protected void basic(P parameters) throws Exception {
		BlockingQueue<ImmutableSession> expiredSessions = new LinkedBlockingQueue<>();
		SessionManagerConfiguration<String> managerConfig1 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		SessionManagerConfiguration<String> managerConfig2 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		try (SessionManagerFactoryProvider<String, B> provider1 = this.factory.apply(parameters, "member1")) {
			try (SessionManagerFactory<String, AtomicReference<String>, B> factory1 = provider1.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
				SessionManager<AtomicReference<String>, B> manager1 = factory1.createSessionManager(managerConfig1);
				manager1.start();
				try (SessionManagerFactoryProvider<String, B> provider2 = this.factory.apply(parameters, "member2")) {
					try (SessionManagerFactory<String, AtomicReference<String>, B> factory2 = provider2.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
						SessionManager<AtomicReference<String>, B> manager2 = factory2.createSessionManager(managerConfig2);
						manager2.start();

						String sessionId = manager1.getIdentifierFactory().get();
						this.verifyNoSession(manager1, sessionId);
						this.verifyNoSession(manager2, sessionId);
						UUID foo = UUID.randomUUID();
						UUID bar = UUID.randomUUID();

						this.createSession(manager1, sessionId, Map.of("foo", foo, "bar", bar));

						this.verifySession(manager1, sessionId, Map.of("foo", foo, "bar", bar));
						this.verifySession(manager2, sessionId, Map.of("foo", foo, "bar", bar));

						this.updateSession(manager1, sessionId, Map.of("foo", new AbstractMap.SimpleEntry<>(foo, null), "bar", Map.entry(bar, 0)));

						for (int i = 1; i <= 20; i += 2) {
							this.updateSession(manager1, sessionId, Map.of("bar", Map.entry(i - 1, i)));
							this.updateSession(manager2, sessionId, Map.of("bar", Map.entry(i, i + 1)));
						}

						this.invalidateSession(manager1, sessionId);

						this.verifyNoSession(manager1, sessionId);
						this.verifyNoSession(manager2, sessionId);

						assertTrue(expiredSessions.isEmpty());

						manager2.stop();
					}
				}
				manager1.stop();
			}
		}
	}

	protected void concurrent(P parameters) throws Exception {
		int threads = 10;
		int requests = 10;
		BlockingQueue<ImmutableSession> expiredSessions = new LinkedBlockingQueue<>();
		SessionManagerConfiguration<String> managerConfig1 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		SessionManagerConfiguration<String> managerConfig2 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		try (SessionManagerFactoryProvider<String, B> provider1 = this.factory.apply(parameters, "member1")) {
			try (SessionManagerFactory<String, AtomicReference<String>, B> factory1 = provider1.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
				SessionManager<AtomicReference<String>, B> manager1 = factory1.createSessionManager(managerConfig1);
				manager1.start();
				try (SessionManagerFactoryProvider<String, B> provider2 = this.factory.apply(parameters, "member2")) {
					try (SessionManagerFactory<String, AtomicReference<String>, B> factory2 = provider2.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
						SessionManager<AtomicReference<String>, B> manager2 = factory2.createSessionManager(managerConfig2);
						manager2.start();

						String sessionId = manager1.getIdentifierFactory().get();
						AtomicInteger value = new AtomicInteger();

						this.createSession(manager1, sessionId, Map.of("value", value));

						// Trigger a number of concurrent sequences of requests for the same session
						CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
						for (int i = 0; i < threads; ++i) {
							future = future.thenAcceptBoth(CompletableFuture.runAsync(() -> {
								for (int j = 0; j < requests; ++j) {
									this.requestSession(manager1, sessionId, session -> {
										AtomicInteger v = (AtomicInteger) session.getAttributes().getAttribute("value");
										assertNotNull(v);
										v.incrementAndGet();
									});
								}
							}), Functions.discardingBiConsumer());
						}
						future.join();

						// Verify integrity of value on other manager
						AtomicInteger expected = new AtomicInteger(threads * requests);
						this.requestSession(manager2, sessionId, session -> {
							// N.B. AtomicInteger does not implement equals(...)
							this.verifySessionAttribute(session, "value", expected, (value1, value2) -> value1.get() == value2.get());
						});

						this.invalidateSession(manager2, sessionId);

						manager2.stop();
					}
				}
				manager1.stop();
			}
		}
	}

	protected void expiration(P parameters) throws Exception {
		Duration expirationDuration = Duration.ofSeconds(120);
		BlockingQueue<ImmutableSession> expiredSessions = new LinkedBlockingQueue<>();
		SessionManagerConfiguration<String> managerConfig1 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		SessionManagerConfiguration<String> managerConfig2 = new TestSessionManagerConfiguration<>(expiredSessions, DEPLOYMENT_CONTEXT);
		try (SessionManagerFactoryProvider<String, B> provider1 = this.factory.apply(parameters, "member1")) {
			try (SessionManagerFactory<String, AtomicReference<String>, B> factory1 = provider1.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
				SessionManager<AtomicReference<String>, B> manager1 = factory1.createSessionManager(managerConfig1);
				manager1.start();
				try (SessionManagerFactoryProvider<String, B> provider2 = this.factory.apply(parameters, "member2")) {
					try (SessionManagerFactory<String, AtomicReference<String>, B> factory2 = provider2.createSessionManagerFactory(SESSION_CONTEXT_FACTORY, CONTAINER_FACADE_PROVIDER)) {
						SessionManager<AtomicReference<String>, B> manager2 = factory2.createSessionManager(managerConfig2);
						manager2.start();

						String sessionId = manager1.getIdentifierFactory().get();
						UUID foo = UUID.randomUUID();
						UUID bar = UUID.randomUUID();

						this.createSession(manager1, sessionId, Map.of("foo", foo, "bar", bar));

						// Setup session to expire soon
						this.requestSession(manager1, sessionId, session -> {
							session.getMetaData().setTimeout(Duration.ofSeconds(3));
						});

						// Verify that session does not expire prematurely
						TimeUnit.SECONDS.sleep(2);

						this.verifySession(manager2, sessionId, Map.of("foo", foo, "bar", bar));

						// Verify that session does not expire prematurely
						TimeUnit.SECONDS.sleep(2);

						this.verifySession(manager2, sessionId, Map.of("foo", foo, "bar", bar));

						try {
							Instant start = Instant.now();
							ImmutableSession expiredSession = expiredSessions.poll(expirationDuration.getSeconds(), TimeUnit.SECONDS);
							assertNotNull(expiredSession, () -> String.format("No expiration event received within %s seconds", expirationDuration.getSeconds()));
							System.out.println(String.format("Received expiration event for %s after %s", expiredSession.getId(), Duration.between(start, Instant.now())));
							assertEquals(sessionId, expiredSession.getId());
							assertFalse(expiredSession.isValid());
							assertFalse(expiredSession.getMetaData().isNew());
							assertTrue(expiredSession.getMetaData().isExpired());
							assertFalse(expiredSession.getMetaData().isImmortal());
							this.verifySessionAttributes(expiredSession, Map.of("foo", foo, "bar", bar));

							assertEquals(0, expiredSessions.size(), expiredSessions.toString());
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}

						this.verifyNoSession(manager1, sessionId);
						this.verifyNoSession(manager2, sessionId);

						manager2.stop();
					}
				}
				manager1.stop();
			}
		}
	}

	private void createSession(SessionManager<AtomicReference<String>, B> manager, String sessionId, Map<String, Object> attributes) {
		this.requestSession(manager, manager::createSession, sessionId, session -> {
			assertTrue(session.getMetaData().isNew());
			this.verifySessionMetaData(session);
			this.verifySessionAttributes(session, Map.of());
			this.updateSessionAttributes(session, attributes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new AbstractMap.SimpleEntry<>(null, entry.getValue()))));
			this.verifySessionAttributes(session, attributes);
		});
	}

	private void verifySession(SessionManager<AtomicReference<String>, B> manager, String sessionId, Map<String, Object> attributes) {
		this.requestSession(manager, sessionId, session -> {
			assertFalse(session.getMetaData().isNew());
			this.verifySessionMetaData(session);
			this.verifySessionAttributes(session, attributes);
		});
	}

	private void updateSession(SessionManager<AtomicReference<String>, B> manager, String sessionId, Map<String, Map.Entry<Object, Object>> attributes) {
		this.requestSession(manager, sessionId, session -> {
			assertNotNull(session);
			assertEquals(sessionId, session.getId());
			assertFalse(session.getMetaData().isNew());
			this.verifySessionMetaData(session);
			this.updateSessionAttributes(session, attributes);
		});
	}

	private void invalidateSession(SessionManager<AtomicReference<String>, B> manager, String sessionId) {
		this.requestSession(manager, sessionId, session -> {
			session.invalidate();
			assertFalse(session.isValid());
			assertThrows(IllegalStateException.class, () -> session.getAttributes());
			assertThrows(IllegalStateException.class, () -> session.getMetaData());
		});
	}

	private void requestSession(SessionManager<AtomicReference<String>, B> manager, String sessionId, Consumer<Session<AtomicReference<String>>> action) {
		this.requestSession(manager, manager::findSession, sessionId, action);
	}

	private void requestSession(SessionManager<AtomicReference<String>, B> manager, Function<String, Session<AtomicReference<String>>> sessionFactory, String sessionId, Consumer<Session<AtomicReference<String>>> action) {
		Instant start = Instant.now();
		try (B batch = manager.getBatcher().createBatch()) {
			try (Session<AtomicReference<String>> session = sessionFactory.apply(sessionId)) {
				assertNotNull(session);
				assertEquals(sessionId, session.getId());
				action.accept(session);
				if (session.isValid()) {
					session.getMetaData().setLastAccess(start, Instant.now());
				}
			}
		}
	}

	private void verifySessionMetaData(Session<AtomicReference<String>> session) {
		assertTrue(session.isValid());
		SessionMetaData metaData = session.getMetaData();
		assertFalse(metaData.isImmortal(), metaData.toString());
		assertFalse(metaData.isExpired(), metaData.toString());
		if (metaData.isNew()) {
			assertNull(metaData.getLastAccessStartTime());
			assertNull(metaData.getLastAccessEndTime());
		} else {
			assertNotNull(metaData.getLastAccessStartTime());
			assertNotNull(metaData.getLastAccessEndTime());
			// For the request following session creation, the last access time will precede the creation time, but this duration should be tiny
			if (metaData.getLastAccessStartTime().isBefore(metaData.getCreationTime())) {
				assertEquals(0, Duration.between(metaData.getLastAccessStartTime(), metaData.getCreationTime()).getSeconds(), metaData.toString());
			}
			assertTrue(metaData.getLastAccessStartTime().isBefore(metaData.getLastAccessEndTime()), metaData.toString());
		}
	}

	private void verifySessionAttributes(ImmutableSession session, Map<String, Object> attributes) {
		assertEquals(attributes.keySet(), session.getAttributes().getAttributeNames());
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			this.verifySessionAttribute(session, entry.getKey(), entry.getValue(), Objects::equals);
		}
	}

	private <T> void verifySessionAttribute(ImmutableSession session, String name, T expected, BiPredicate<T, T> equals) {
		@SuppressWarnings("unchecked")
		T value = (T) session.getAttributes().getAttribute(name);
		assertTrue(equals.test(expected, value), () -> String.format("Expected %s, Actual %s", expected, value));
	}

	private void updateSessionAttributes(Session<AtomicReference<String>> session, Map<String, Map.Entry<Object, Object>> attributes) {
		for (Map.Entry<String, Map.Entry<Object, Object>> entry : attributes.entrySet()) {
			String name = entry.getKey();
			Object expected = entry.getValue().getKey();
			Object value = entry.getValue().getValue();
			if (value != null) {
				assertEquals(expected, session.getAttributes().setAttribute(name, value));
			} else {
				assertEquals(expected, session.getAttributes().removeAttribute(name));
			}
		}
	}

	private void verifyNoSession(SessionManager<AtomicReference<String>, B> manager, String sessionId) {
		try (B batch = manager.getBatcher().createBatch()) {
			try (Session<AtomicReference<String>> session = manager.findSession(sessionId)) {
				assertNull(session);
			}
		}
	}

	private static class TestSessionManagerConfiguration<DC> implements SessionManagerConfiguration<DC> {
		private final BlockingQueue<ImmutableSession> expired;
		private final DC context;

		TestSessionManagerConfiguration(BlockingQueue<ImmutableSession> expired, DC context) {
			this.expired = expired;
			this.context = context;
		}

		@Override
		public Supplier<String> getIdentifierFactory() {
			return () -> UUID.randomUUID().toString();
		}

		@Override
		public Consumer<ImmutableSession> getExpirationListener() {
			return this.expired::add;
		}

		@Override
		public Duration getTimeout() {
			return Duration.ofMinutes(1);
		}

		@Override
		public DC getContext() {
			return this.context;
		}
	}
}
