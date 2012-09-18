package org.princehouse.mica.example.modularpipe;

import java.util.Map;

import org.princehouse.mica.base.net.model.Address;

public abstract class MapModule<S> extends Module<S, Map<Address,S>> {
	
	private int n;
	private int t;
	
	public MapModule(Object view, int n, int t) {
		super(view);
		this.n = n;
		this.t = t;
	}

	@Override
	public boolean ready(Map<Address, S> aggregate) {
		return aggregate.size() >= (n-t);
	}

	@Override
	public Map<Address, S> resetAggregate(Map<Address,S> base) {
		base.clear();
		return base;
	}

	@Override
	public Map<Address, S> updateAggregate(Map<Address, S> aggregate, S summary) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public S getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void advance(Address address, Map<Address, S> aggregate) {
		// TODO Auto-generated method stub
		
	}
}


