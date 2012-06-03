package org.princehouse.mica.base;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.MarkingObjectInputStream;

/**
 * Base for all MiCA protocols.  Extend this class to create your own protocol.
 * 
 * @author lonnie
 *
 */
public abstract class BaseProtocol implements Protocol, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7604139731035133018L;

	/**
	 * Default constructor
	 */
	public BaseProtocol() {}

	// clunky mechanism to register "foreign" objects when they are deserialized at a remote node
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(in instanceof MarkingObjectInputStream) {
			((MarkingObjectInputStream) in).getForeignObjectSet().add(this);
		} 
	}

	@Override
	public final RuntimeState getRuntimeState() {
		return Runtime.getRuntime().getRuntimeState(this);
	}

	@Override
	public String toString() {
		try {
			return String.format("[%s@%s]", getClass().getName(), getRuntimeState().getAddress());  
		} catch (RuntimeException e) {
			return "[!]";
		}
	}

	@Override
	final public Distribution<Address> getSelectDistribution() {
		try {
			return Runtime.getRuntime().getSelectDistribution(this);
		} catch (SelectException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Get the current node's address. This is part of Runtime state.
	 * @return Current node's address
	 */
	public Address getAddress() {
		return getRuntimeState().getAddress();
	}

	@Override
	public void executeUpdate(Protocol other) {
		Runtime.getRuntime().executeUpdate(this,other);
	}


	@Override 
	public double getFrequency() {
		return Runtime.getRuntime().getRate(this);
	}

	// TODO "local_timestamp":  Is that in ms or rounds?
	/**
	 * Write a message to the log.  Log messages are comma-separated fields of the format:
	 * 
	 * "local_timestamp,local_event_number,address,classname,name,MESSAGE"
	 * 
	 * Where MESSAGE is the result of String.format(formatStr,arguments).
	 * 
	 * @param formatStr
	 * @param arguments
	 */
	

	public static class InstanceLogObject {
		public Object data;
	}
	
	/**
	 * Convenience method for logging only an event type
	 * @param eventType
	 */
	public void logJson(String eventType) {
		Runtime.getRuntime().logJson(eventType);
	}
	
	public void logJson(String eventType, final Object obj) {
		InstanceLogObject logobj = new InstanceLogObject();
		logobj.data = obj;
		Runtime.getRuntime().logJson(getAddress(), eventType, logobj);
	}

	/**
	 * The default rate of all protocols is 1.0.
	 * Override this only if you specifically want to make this protocol gossip at a 
	 * non-uniform rate (i.e., merge operators do this)
	 * @return
	 */
	@GossipRate
	public double rate() {
		return 1.0;
	}

	@Override
	public Object getLogState() {
		return this;
	}

	@Override 
	public void preUpdate(Address selected) {}
	
	@Override
	public void postUpdate() {}
	
}
