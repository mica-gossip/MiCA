package org.princehouse.mica.lib;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Broadcast;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

public abstract class NaiveBroadcast<Message extends Serializable> extends BaseProtocol implements Broadcast<Message> {

	private static final long serialVersionUID = 1L;

	@SelectUniformRandom
	public Overlay overlay;
	
	public NaiveBroadcast(Overlay overlay) {
		this.overlay = overlay;
	}
	
	private Map<Address,List<Message>> sendQueues = Functional.map();
	
	// Seriously inefficient: Keep a set of all the messages we've ever received, so we can compare them with incoming
	// messages
	private Set<Message> received = new HashSet<Message>();
	
	@Override
	public void sendMessage(Message m) {
		received.add(m);
		Distribution<Address> dist = getSelectDistribution();
		Set<Address> view = new HashSet<Address>();
		for(Map.Entry<Address,Double> entry  : dist.entrySet()) {
			// enumerate nodes with non-zero probability of selection
			if(entry.getValue() > 0) 
				view.add(entry.getKey());
		}
		
		// queue the message to send to everyone in the current view
		for(Address a : view) {
			List<Message> q = null;
			if(!sendQueues.containsKey(a)) {
				q = new LinkedList<Message>();
				sendQueues.put(a, q);
			}
		}
		
		// purge non-view members from the send queues
		for(Address a : Functional.list(sendQueues.keySet())) {
			if(!view.contains(a))
				sendQueues.remove(a);
		}
	}

	
	@GossipUpdate
	public void update(NaiveBroadcast<Message> other) {
		Address oa = other.getAddress();
		if(!sendQueues.containsKey(oa)) 
			return;
		for(Message m : sendQueues.get(oa)) {
			if(!other.received.contains(m)) {
				other.sendMessage(m);
				other.receiveMessage(m);
			}
		}
	}
	
	/**
	 * Subclasses must implement this function
	 */
	@Override
	abstract public void receiveMessage(Message m);
	
}
