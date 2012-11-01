package org.princehouse.mica.example.dolev;

import java.io.Serializable;

import org.princehouse.mica.base.net.model.Address;

public class LSSMMessage implements Comparable<LSSMMessage>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum MessageSource {
		ORIGIN, DIRECT, INDIRECT;
	};
	
	public Address peer;
	public long timestamp;
	public LSSMMessage.MessageSource source;
	public Object state;


	public LSSMMessage(Address peer, long timestamp, Object state, LSSMMessage.MessageSource s) {
		assert(peer != null);
		
		this.peer = peer;
		this.timestamp = timestamp;
		this.source = s;
		this.state = state;
	}

	public LSSMMessage tell() {
		// copy this message and add one level of indirection
		LSSMMessage.MessageSource s = null;
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
		
		return new LSSMMessage(peer, timestamp, state, s);
	}

	@Override
	public int compareTo(LSSMMessage other) {
		// sort from greatest to least time stamp
		
		int v = -(Long.valueOf(timestamp).compareTo(
				Long.valueOf(other.timestamp)));
		if(v != 0) 
			return v;
		// break ties by comparing directness.  More direct < less direct
		return source.compareTo(other.source);
	}
}