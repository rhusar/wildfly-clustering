/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.infinispan.embedded.user;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Formatter;
import org.wildfly.clustering.session.infinispan.embedded.SessionKeyFormatter;

/**
 * Formatter for {@link UserSessionsKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class UserSessionsKeyFormatter extends SessionKeyFormatter<UserSessionsKey> {

	public UserSessionsKeyFormatter() {
		super(UserSessionsKey.class, UserSessionsKey::new);
	}
}
