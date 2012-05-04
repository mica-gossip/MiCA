package org.princehouse.mica.lib;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;

/**
 * Dijkstra's first self-stabilizing token ring example.
 * Moves a token around a ring counter-clockwise (i.e., from successor to predecessor)
 * 
 * Override act() if you want the node to do something when it has the token.
 *  
 * @author lonnie
 *
 */
public abstract class TokenRing extends BaseProtocol {

	private static final long serialVersionUID = 1L;

	@Select
	public SinglyLinkedRingOverlay ring = null;
	private LeaderElection leader = null;
	private NetworkSizeCounter kcount = null;

	private int counter = 0;
	private boolean hasTokenStatus = false;

	public boolean hasToken() {
		return hasTokenStatus;
	}

	public TokenRing(SinglyLinkedRingOverlay ring, LeaderElection leader, NetworkSizeCounter k) {
		this.leader = leader;
		this.ring = ring;
		this.kcount = k;
	}

	@Override
	public void preUpdate(Address selected) {
		hasTokenStatus = false;
	}

	@GossipUpdate
	public void update(TokenRing that) {
		int k = Math.max(1,kcount.size());
		counter %= k;
		int thatCounter = that.counter % k;
		if(leader.isLeader()) {
			if(counter == thatCounter) {
				// Special case where this node is the leader:  
				// If it sees its own or a higher value in its neighbor, increment counter and hold token
				hasTokenStatus = true;
				counter = (counter + 1) % k;
			}
		} else if(counter != thatCounter) {
			hasTokenStatus = true;
			counter = thatCounter;
		}
	}

	@Override 
	public void postUpdate() {
		if(hasToken()) 
			act();
	}

	/**
	 * Called immediately following a gossip exchange if this node is holding the token
	 */
	public void act() {
		// override me
	}


}
