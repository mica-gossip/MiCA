package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.net.model.Address;

public interface LeaderElection {
	public Address getLeader();
	
	public boolean isLeader();

}
