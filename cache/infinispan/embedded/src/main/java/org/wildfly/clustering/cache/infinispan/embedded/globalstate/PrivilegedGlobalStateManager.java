/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.globalstate;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.ScopedPersistentState;
import org.infinispan.globalstate.impl.GlobalStateManagerImpl;

/**
 * Workaround for ISPN-14051.
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
@SuppressWarnings("synthetic-access")
public class PrivilegedGlobalStateManager extends GlobalStateManagerImpl {
	@Override
	public void start() {
		AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Void run() {
				PrivilegedGlobalStateManager.super.start();
				return null;
			}
		});
	}

	@Override
	public void stop() {
		AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Void run() {
				PrivilegedGlobalStateManager.super.stop();
				return null;
			}
		});
	}

	@Override
	public void writeGlobalState() {
		AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Void run() {
				PrivilegedGlobalStateManager.super.writeGlobalState();
				return null;
			}
		});
	}

	@Override
	public void writeScopedState(ScopedPersistentState state) {
		AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Void run() {
				PrivilegedGlobalStateManager.super.writeScopedState(state);
				return null;
			}
		});
	}

	@Override
	public void deleteScopedState(String scope) {
		AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Void run() {
				PrivilegedGlobalStateManager.super.deleteScopedState(scope);
				return null;
			}
		});
	}

	@Override
	public Optional<ScopedPersistentState> readScopedState(String scope) {
		return AccessController.doPrivileged(new PrivilegedAction<>() {
			@Override
			public Optional<ScopedPersistentState> run() {
				return PrivilegedGlobalStateManager.super.readScopedState(scope);
			}
		});
	}
}
