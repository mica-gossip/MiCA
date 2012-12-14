package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.ViewUniformRandom;

/**
 * "Find minimum value" pull protocol example
 * 
 * @author lonnie
 *
 */
public class FindMinPush extends BaseProtocol implements Serializable {
	private static final long serialVersionUID = 1L;

	private int x; 

	@ViewUniformRandom
	public Set<Address> view;
	
	public FindMinPush(int x, Set<Address> view) {
		this.x = x;
		this.view = view;
	}
	
	@GossipUpdate
	@Override
	public void update(Protocol other) {
		FindMinPush o = (FindMinPush) other;
		int temp = Math.min(x, o.x);		
		System.out.printf("execute push update (%s,%s):  (%d,%d) -> (%d,%d)\n", this, other, x, o.x, x,temp );
		o.x = temp;
	}


}
