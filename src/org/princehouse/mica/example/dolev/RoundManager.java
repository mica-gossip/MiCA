package org.princehouse.mica.example.dolev;

import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;

public class RoundManager extends BaseProtocol {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int f = 0;
	
	private Set<Address> reached = Functional.set();
	
	@View
	public Overlay overlay = null;
	
	// number of nodes in network
	private int n;
	
	public RoundManager(Overlay overlay, int n, int f) {
		this.f = f;
		this.n = n;
		this.overlay = overlay;
	}

	@GossipUpdate
	public void update(RoundManager that) {
		reached.add(that.getAddress());
		that.reached.add(this.getAddress());
	}

	public boolean ready() {
		return getRemainingCount() <= 0;
	}
	
	public int getRemainingCount() {
		return (n - f) - reached.size();
	}
	
	public void reset() {
		reached.clear();
	}
	
}
