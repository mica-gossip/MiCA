package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

import com.beust.jcommander.Parameter;

/**
 * Pull-based "find minimum value" example protocol
 * 
 * Demonstrates gossiping to neighbors with an arbitrary weighted distribution
 * 
 * @author lonnie
 *
 */
public class FindMinPullWeighted extends BaseProtocol implements Serializable {

	private static final long serialVersionUID = 1L;

	@Parameter(names = "-x", description="Initial value")
	private int x = 0;
	
	@Parameter(names = "-neighbor", description = "Add a neighbor. Specify multiple times for multiple neighbors.")
	public List<Address> view = new ArrayList<Address>();
	
	/**
	 * Example of an arbitrary select distribution, instead of the uniform random
	 * @return
	 */
	@Select
	public Distribution<Address> select() {
		// Gossip with each subsequent neighbor with increasing frequency
		Distribution<Address> dist = new Distribution<Address>();
		for(int i = 0; i < view.size(); i++) {
			dist.put(view.get(i), (double) (i+1) );
		}
		return dist.normalize();  // Note: It is very important to normalize the distribution before returning it!
	}
	
	
	public FindMinPullWeighted(int x, List<Address> view) {
		this.x = x;
		this.view = view;
	}

	public FindMinPullWeighted() {
	}
	
	@GossipUpdate
	public void update(FindMinPullWeighted other) {
		FindMinPullWeighted o = (FindMinPullWeighted) other;
		int temp = Math.min(x, o.x);		
		x = temp;
	}
		
	@Override public String getStateString() {
		return String.format("%d", x);
	}

}
