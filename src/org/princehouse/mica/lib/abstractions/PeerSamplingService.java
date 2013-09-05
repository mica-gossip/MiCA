package org.princehouse.mica.lib.abstractions;

import java.util.Set;

import org.princehouse.mica.base.net.model.Address;

public interface PeerSamplingService {

	public Set<Address> getRandomPeers();
}
