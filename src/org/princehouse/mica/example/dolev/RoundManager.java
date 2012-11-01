package org.princehouse.mica.example.dolev;

import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

public class PulseRoundManager extends BaseProtocol {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int f = 0;

	private List<Address> neighbors = null;
	private int cursor = 0;
	
	private Set<Address> reached = Functional.set();

	public PulseRoundManager(List<Address> neighbors, int f) {
		this.f = f;
		this.neighbors = neighbors;
	}
	
	@View
	public Address nextPeer() {
		int n = neighbors.size();
		return neighbors.get( cursor % n);
	}

	@GossipUpdate
	public void update(PulseRoundManager that) {
		reached.add(that.getAddress());
		that.reached.add(this.getAddress());
	}

	@Override
	public void preUpdate(Address addr) { 
		cursor++;
	}
	
	public boolean ready() {
		return getRemainingCount() <= 0;
	}
	
	public int getRemainingCount() {
		int n = neighbors.size();
		return (n - f) - reached.size();
	}
	
	public void reset() {
		reached.clear();
	}
	
}
