package org.princehouse.mica.lib;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.ViewUniformRandom;
import org.princehouse.mica.lib.abstractions.Broadcast;
import org.princehouse.mica.lib.abstractions.Overlay;

public abstract class NaiveBroadcast<Message extends Serializable> extends BaseProtocol implements Broadcast<Message> {

	private static final long serialVersionUID = 1L;

	@ViewUniformRandom
	public Overlay overlay;
		
	public int ttl; // newly received messages will be broadcast this many times before we stop sending them to neighbors
	
	public NaiveBroadcast(Overlay overlay, int ttl) {
		this.overlay = overlay;
		this.ttl = ttl;
	}
		
	// Seriously inefficient: Keep a set of all the message hashes we've ever received, so we can compare them with incoming
	// messages
	public Set<Integer> knownMessageHashes = new HashSet<Integer>();
	
	public static class MessageWithTTL<Message extends Serializable> implements Serializable {
		private static final long serialVersionUID = 1L;
		public Message message;
		public int ttl;
		public MessageWithTTL(Message m, int ttl) {
			this.message = m;
			this.ttl = ttl;
		}
	}
	
	// messages with ttl > 0 
	public LinkedList<MessageWithTTL<Message>> outbox = new LinkedList<MessageWithTTL<Message>>();
			
	 /* 
	 * Called by user to originate a message
	 */
	@Override
	public void sendMessage(Message m) {
		receiveMessageInternal(m);
	}
	
	public void receiveMessageInternal(Message m) {
		int hash = m.hashCode();
		if(knownMessageHashes.contains(hash)) {
			return;
		}	
		knownMessageHashes.add(hash);
		outbox.add(new MessageWithTTL<Message>(m, ttl));
		receiveMessage(m);
	}
	
	
	@GossipUpdate
	@Override
	public void update(Protocol that) {
		@SuppressWarnings("unchecked")
		NaiveBroadcast<Message> other = (NaiveBroadcast<Message>) that;
		Address oa = other.getAddress();
		
		LinkedList<MessageWithTTL<Message>> newout = new LinkedList<MessageWithTTL<Message>>();
		
		for(MessageWithTTL<Message> mt : outbox) {
			other.receiveMessageInternal(mt.message);
			mt.ttl -= 1;
			if(mt.ttl > 0) {
				newout.add(mt);
			}
		}
		outbox = newout;
	}
	
	/**
	 * Subclasses must implement this function
	 */
	@Override
	abstract public void receiveMessage(Message m);
	
}
