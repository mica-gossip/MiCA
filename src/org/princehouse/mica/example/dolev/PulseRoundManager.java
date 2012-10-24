package org.princehouse.mica.example.dolev;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public class PulseRoundManager extends BaseProtocol {

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
		return neighbors.get( (cursor++) % n);
	}

	@GossipUpdate
	public void update(PulseRoundManager that) {
		reached.add(that.getAddress());
		that.reached.add(this.getAddress());
	}

	public boolean ready() {
		int n = neighbors.size();
		return reached.size() >= n - f;
	}
	
	public void reset() {
		reached.clear();
	}
	
}
