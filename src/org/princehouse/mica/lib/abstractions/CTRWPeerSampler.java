package org.princehouse.mica.lib.abstractions;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;


/**
 * Peer sampling protocol based on the paper
 * 
 * "Peer counting and sampling in overlay networks based on random walks" 
 * by Ganesh, Kermarrec, Merrer, Massoulie
 * 
 * 
 * @author lonnie
 *
 */
abstract public class CTRWPeerSampler extends BaseProtocol {
	private static final long serialVersionUID = 1L;

	public static class SamplingMessage {
		public Address source;
		public double timer;
		public void update(int degree) { 
			double u = new Random().nextDouble();
			timer -= -Math.log10(u) / (double) degree;
		}
		public boolean finished() {
			return timer <= 0;
		}
	}
	
	public Queue<SamplingMessage> messages = new LinkedList<SamplingMessage>();
	
	@SelectUniformRandom
	public Set<Address> select() {
		SamplingMessage m = messages.peek(); // next message in the queue
		if(m == null) 
			return null; // No messages.  Do not gossip.
		else if(m.finished()) {
			Set<Address> s = new HashSet<Address>();
			s.add(m.source);
			return s;
		} else {
			return view;
		}		
	}
	
	@SelectUniformRandom
	public Set<Address> view;
	
	public CTRWPeerSampler(Set<Address> view) {
		this.view = view;
	}
	
	@GossipUpdate
	public void exchange(CTRWPeerSampler other) {
		SamplingMessage m = messages.poll(); 
		if(m.finished()) {
			other.samplePeer(this);
		} else {
			// pass the message along to a neighbor			
			m.update(getDegree());
			other.messages.add(m);
		}
	}
	
	/**
	 * Compute a sample from the peer determined by the random walk
	 */
	abstract public void samplePeer(CTRWPeerSampler peer);
	
	public int getDegree() {
		return view.size();
	}
	
	

}
