/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.jgroups;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.wildfly.clustering.server.AutoCloseableProvider;
import org.wildfly.clustering.server.group.Group;

/**
 * @author Paul Ferraro
 */
public class JChannelGroupProvider extends AutoCloseableProvider implements GroupProvider<Address, ChannelGroupMember> {

	private final JChannel channel;
	private final ChannelGroup group;

	public JChannelGroupProvider(String clusterName, String memberName) {
		this.channel = JChannelFactory.INSTANCE.apply(memberName);
		this.accept(this.channel::close);
		try {
			this.channel.connect(clusterName);
			this.accept(this.channel::disconnect);
			this.group = new JChannelGroup(this.channel);
			this.accept(this.group::close);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JChannel getChannel() {
		return this.channel;
	}

	@Override
	public Group<Address, ChannelGroupMember> getGroup() {
		return this.group;
	}
}
