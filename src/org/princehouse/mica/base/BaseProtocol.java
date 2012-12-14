package org.princehouse.mica.base;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.Sugar;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
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
	public RuntimeState getRuntimeState() {
		return MiCA.getRuntimeInterface().getRuntimeContextManager().getRuntimeState(this);
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
	public Distribution<Address> getView() {
		return Sugar.v().executeSugarView(this);
	}
	
	@Override 
	public double getRate() {
		return Sugar.v().executeSugarRate(this);
	}

	/**
	 * Get the current node's address. This is part of Runtime state.
	 * @return Current node's address
	 */
	@Override
	public Address getAddress() {
		return getRuntimeState().getAddress();
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
	public void logJson(Object flags, String eventType) {
		logJson(flags, eventType, null);
	}
	
	public void logJson(Object flags, String eventType, final Object obj) {
		MiCA.getRuntimeInterface().logJson(flags, getAddress(), eventType, obj);
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
