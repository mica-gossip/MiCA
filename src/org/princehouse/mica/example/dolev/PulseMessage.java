package org.princehouse.mica.example.dolev;

import org.princehouse.mica.base.net.model.Address;

public class PulseMessage implements Comparable<PulseMessage> {
	public static enum MessageSource {
		ORIGIN, DIRECT, INDIRECT;
	};

	public PulseMessage(Address peer, long timestamp, PulseState state, PulseMessage.MessageSource s) {
		this.peer = peer;
		this.timestamp = timestamp;
		this.source = s;
		this.state = state;
	}

	public Address peer;
	public long timestamp;
	public PulseMessage.MessageSource source;
	public PulseState state;
	
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
		// sort from greatest to least time stamps
		int v = -(Long.valueOf(timestamp).compareTo(
				Long.valueOf(other.timestamp)));
		if(v != 0) 
			return v;
		// break ties by comparing directness.  More direct < less direct
		return source.compareTo(other.source);
	}
}