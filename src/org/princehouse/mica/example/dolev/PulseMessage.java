package org.princehouse.mica.example.dolev;

import java.io.Serializable;

import org.princehouse.mica.base.net.model.Address;

public class PulseMessage implements Comparable<PulseMessage>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum MessageSource {
		ORIGIN, DIRECT, INDIRECT;
	};
	
	public Address peer;
	public long timestamp;
	public PulseMessage.MessageSource source;
	public Object state;


	public PulseMessage(Address peer, long timestamp, Object state, PulseMessage.MessageSource s) {
		assert(peer != null);
		
		this.peer = peer;
		this.timestamp = timestamp;
		this.source = s;
		this.state = state;
	}

	public PulseMessage tell() {
		// copy this message and add one level of indirection
		PulseMessage.MessageSource s = null;
		switch (source) {
		case ORIGIN:
			s = MessageSource.DIRECT;
			break;
		case DIRECT:
			s = MessageSource.INDIRECT;
			break;
		case INDIRECT:
			s = MessageSource.INDIRECT;
			break;
		};
		
		return new PulseMessage(peer, timestamp, state, s);
	}

	@Override
	public int compareTo(PulseMessage other) {
		// sort from greatest to least time stamp
		
		int v = -(Long.valueOf(timestamp).compareTo(
				Long.valueOf(other.timestamp)));
		if(v != 0) 
			return v;
		// break ties by comparing directness.  More direct < less direct
		return source.compareTo(other.source);
	}
}