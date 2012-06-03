package org.princehouse.mica.example;


import org.princehouse.mica.base.ExternalSelectProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.lib.abstractions.Overlay;

import com.beust.jcommander.Parameter;

/**
 * Pull-based "find minimum value" example protocol
 * 
 * @author lonnie
 *
 */
public class FindMinPull extends ExternalSelectProtocol {

	private static final long serialVersionUID = 1L;

	@Parameter(names = "-x", description="Initial value")
	private int x = 0;

	
	public FindMinPull(int x, Overlay overlay) {
		super(overlay);
		this.x = x;
	}

	@GossipUpdate
	public void update(FindMinPull other) {
		FindMinPull o = (FindMinPull) other;
		int temp = Math.min(x, o.x);		
		x = temp;
	}

}
