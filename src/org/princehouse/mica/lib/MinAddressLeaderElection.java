package org.princehouse.mica.lib;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.Overlay;

public class MinAddressLeaderElection extends BaseProtocol implements LeaderElection {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Address leader;

	@SelectUniformRandom
	public Overlay overlay;
	
	public MinAddressLeaderElection(Overlay overlay) {
		this.overlay = overlay;
	}
	
	@Override
	public Address getLeader() {
		if(leader == null) 
			leader = getRuntimeState().getAddress();;
		return leader;
	}

	public void setLeader(Address a) {
		leader = a;
	}
	
	@Override
	public boolean isLeader() {
		return getLeader().equals(getRuntimeState().getAddress());
	}

	@GossipUpdate
	public void update(MinAddressLeaderElection other) {
		Address a = getLeader();
		Address b = other.getLeader();
		
		// the greatest address gets to be leader
		if(a.compareTo(b) > 0)
			leader = b;
		else {
			other.leader = a;
		}
	}

	@Override
	public String getStateString() {
		return String.format("%s,",getLeader());
	}
}
