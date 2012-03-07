package org.princehouse.mica.example;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;

import com.beust.jcommander.Parameter;

/**
 * Pull-based "find minimum value" example protocol
 * 
 * @author lonnie
 *
 */
public class FindMinPull extends BaseProtocol implements Serializable {

	private static final long serialVersionUID = 1L;

	@Parameter(names = "-x", description="Initial value")
	private int x = 0;
	
	@SelectUniformRandom
	@Parameter(names = "-neighbor", description = "Add a neighbor. Specify multiple times for multiple neighbors.")
	public List<Address> view = new ArrayList<Address>();
	
	public FindMinPull(int x, List<Address> view) {
		this.x = x;
		this.view = view;
	}

	public FindMinPull() {
	}
	
	@GossipUpdate
	public void update(FindMinPull other) {
		FindMinPull o = (FindMinPull) other;
		int temp = Math.min(x, o.x);		
		x = temp;
	}
		
	@Override public String getStateString() {
		return String.format("%d", x);
	}

}
