package org.princehouse.mica.lib;


import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;


public class FindMinPull extends BaseProtocol implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int x; 

	@SelectUniformRandom
	public Set<Address> view;
	
	public FindMinPull(int x, Set<Address> view) {
		this.x = x;
		this.view = view;
	}

	@GossipUpdate
	public void update(FindMinPull other) {
		FindMinPull o = (FindMinPull) other;
		int temp = Math.min(x, o.x);		
		System.out.printf("execute pull update (%s,%s):  (%d,%d) -> (%d,%d)\n", this, other, x, o.x, temp,o.x);
		x = temp;
	}

}
