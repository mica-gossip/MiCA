package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;


/**
 * Push-pull "find minimum value" example
 * @author lonnie
 */
public class FindMinSymmetric extends BaseProtocol implements Serializable {
	private static final long serialVersionUID = 1L;

	public int x; 

	@SelectUniformRandom
	public Set<Address> view;
	
	public FindMinSymmetric(int x, Set<Address> view) {
		this.x = x;
		this.view = view;
	}
	
	@GossipUpdate
	public void update(FindMinSymmetric other) {
		FindMinSymmetric o = (FindMinSymmetric) other;
		int temp = Math.min(x, o.x);		
		//System.out.printf("execute symmetric update (%s,%s):  (%d,%d) -> (%d,%d)\n", this, other, x, o.x, temp,temp );
		x = temp;
		o.x = temp;
	}

}
