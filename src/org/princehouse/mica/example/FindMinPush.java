package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;

/**
 * "Find minimum value" pull protocol example
 * 
 * @author lonnie
 *
 */
public class FindMinPush extends BaseProtocol implements Serializable {
	private static final long serialVersionUID = 1L;

	private int x; 

	@SelectUniformRandom
	public Set<Address> view;
	
	public FindMinPush(int x, Set<Address> view) {
		this.x = x;
		this.view = view;
	}
	
	@GossipUpdate
	public void update(FindMinPush other) {
		FindMinPush o = (FindMinPush) other;
		int temp = Math.min(x, o.x);		
		System.out.printf("execute push update (%s,%s):  (%d,%d) -> (%d,%d)\n", this, other, x, o.x, x,temp );
		o.x = temp;
	}


}
