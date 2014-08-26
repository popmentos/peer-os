package org.safehaus.subutai.product.common.test.unit.mock;


import com.google.common.collect.Sets;
import org.safehaus.subutai.shared.protocol.Agent;

import java.util.*;


public class CommonMockBuilder {

	public static Map<Agent, Set<Agent>> getLxcMap() {
		Agent agent = CommonMockBuilder.createAgent();
		Map<Agent, Set<Agent>> lxcMap = new HashMap<>();

		lxcMap.put(agent, Sets.newHashSet(agent));

		return lxcMap;
	}

	public static Agent createAgent() {
		return new Agent(UUID.randomUUID(), "127.0.0.1", "", "00:00:00:00", Arrays.asList("127.0.0.1", "127.0.0.1"),
				true, "transportId");
	}

}
