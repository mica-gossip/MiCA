package org.princehouse.mica.lib;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;

/*
 * This is the first of three self-stabilizing token rings proposed in Dijkstra's seminal self-stabiliaztion paper.
 * 
 * The algorithm is very simple.  
 *   Let k = 1 + number of nodes in the network
 *   Each node stores a counter, 0 <= counter < k
 *   Nodes are arranged in a ring, such that each node nodes its successor.
 *      (the implementation below relies on an external SinglyLinkedRingOverlay instance to provide the ring).
 *   
 * An external oracle distinguishes one node as leader.  This implementation uses an external LeaderElection instance to provide this.
 * 
 * The gossip exchange is as follows. Let C be the local node's counter and SUCC be its successor's counter.
 * 
 * if isLeader():
 *   if C == SUCC: 
 *     C += 1
 * else:
 *   if SUCC != C: 
 *     C = SUCC
 * 
 * (Note that all values are mod (k+1) ) 
 * 
 * If a node modifies its counter in a given round, it is said that that node has the token for that round.
 * In the asynchronous MiCA setting, this translates into a hand-off of the token from successor to predecessor.  Multiple nodes
 * may have the token in the same round-length time unit, but no two nodes have it at the same instant.
 * 
 * The implementation calls an act() function following a gossip update in which it has the token.
 * 
 * This converges in a maximum of k rounds.  Note that this is not very impressive.  Dijkstra's subsequent two self-stabilizing 
 * token ring protocols converge in a fixed number of rounds independent of network size, but are more complicated.
 * 
 */

/**
 * Dijkstra's first self-stabilizing token ring example. Moves a token around a
 * ring counter-clockwise (i.e., from successor to predecessor)
 * 
 * Override act() if you want the node to do something when it has the token.
 *
 * Depends on several input services: 
 *   ring, a SinglyLinkedRingOverlay
 *   leader, a LeaderElection implementation
 *   networkSize, a NetworkSizeCounter implementation 
 *   
 * @author lonnie
 * 
 */
public abstract class TokenRing extends BaseProtocol {

	private static final long serialVersionUID = 1L;

	@View
	public SinglyLinkedRingOverlay ring = null;
	private LeaderElection leader = null;
	private NetworkSizeCounter kcount = null;

	private int counter = 0;
	private boolean hasTokenStatus = false;

	public boolean hasToken() {
		return hasTokenStatus;
	}

	/**
	 * 
	 * @param ring A singly-linked ring overlay
	 * @param leader A LeaderElection mechanism
	 * @param k For correctness, k must be >= number of nodes in the network
	 */
	public TokenRing(SinglyLinkedRingOverlay ring, LeaderElection leader,
			NetworkSizeCounter k) {
		this.leader = leader;
		this.ring = ring;
		this.kcount = k; // size of network
	}

	@Override
	public void preUpdate(Address selected) {
		hasTokenStatus = false;
	}

	@GossipUpdate
	@Override
	public void update(Protocol other) {
		TokenRing that = (TokenRing) other;
		int k = Math.max(1, kcount.size()) + 1;
		counter %= k;
		int thatCounter = that.counter % k;
		if (leader.isLeader()) {
			if (counter == thatCounter) {
				hasTokenStatus = true;
				counter = (counter + 1) % k;
			}
		} else if (counter != thatCounter) {
			hasTokenStatus = true;
			counter = thatCounter;
		}
	}

	@Override
	public void postUpdate() {
		if (hasToken())
			act();
	}

	/**
	 * Called immediately following a gossip exchange if this node is holding
	 * the token
	 */
	public void act() {
		// override me
	}

}
