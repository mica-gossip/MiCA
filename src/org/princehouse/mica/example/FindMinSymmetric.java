package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.ViewUniformRandom;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;


/**
 * Push-pull "find minimum value" example
 * @author lonnie
 */
public class FindMinSymmetric extends BaseProtocol implements Serializable {
	private static final long serialVersionUID = 1L;

	public int x; 

	@ViewUniformRandom
	public Set<Address> view;
	
	public FindMinSymmetric(int x, Set<Address> view) {
		this.x = x;
		this.view = view;
	}
	
	@GossipUpdate
	@Override
	public void update(Protocol other) {
		FindMinSymmetric o = (FindMinSymmetric) other;
		int temp = Math.min(x, o.x);		
		//System.out.printf("execute symmetric update (%s,%s):  (%d,%d) -> (%d,%d)\n", this, other, x, o.x, temp,temp );
		x = temp;
		o.x = temp;
	}

}
