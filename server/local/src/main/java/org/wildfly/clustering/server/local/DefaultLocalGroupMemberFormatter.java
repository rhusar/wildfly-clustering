/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.local;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Formatter;
import org.wildfly.clustering.marshalling.SimpleFormatter;

/**
 * Resolver for a {@link DefaultLocalGroupMember}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class DefaultLocalGroupMemberFormatter extends SimpleFormatter<DefaultLocalGroupMember> {

	public DefaultLocalGroupMemberFormatter() {
		super(DefaultLocalGroupMember.class, DefaultLocalGroupMember::new, DefaultLocalGroupMember::getName);
	}
}
