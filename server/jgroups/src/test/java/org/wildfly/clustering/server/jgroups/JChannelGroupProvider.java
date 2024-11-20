/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.jgroups;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.wildfly.clustering.server.group.Group;

/**
 * @author Paul Ferraro
 */
public class JChannelGroupProvider implements GroupProvider<Address, ChannelGroupMember> {

	private final JChannel channel;
	private final ChannelGroup group;

	public JChannelGroupProvider(String clusterName, String memberName) throws Exception {
		this.channel = JChannelFactory.INSTANCE.apply(memberName);
		this.channel.connect(clusterName);
		this.group = new JChannelGroup(this.channel);
	}

	@Override
	public JChannel getChannel() {
		return this.channel;
	}

	@Override
	public Group<Address, ChannelGroupMember> getGroup() {
		return this.group;
	}

	@Override
	public void close() {
		this.group.close();
		this.channel.disconnect();
		this.channel.close();
	}
}
