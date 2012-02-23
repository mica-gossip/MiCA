package org.princehouse.mica.base.model;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

/**
 * Don't use this directly.
 * Extend BaseProtocol to implement your own protocols.
 * 
 * @author lonnie
 *
 */
public interface Protocol {
	
	/**
	 * RuntimeState is location-specific state that is independent of the protocol instance.
	 * This includes the runtime clock and random number generator.
	 * 
	 * @return Local node's runtime state.
	 */
	public RuntimeState getRuntimeState();
	
	/**
	 * Execute the select function for this Protocol instance and return its address distribution.
	 * @return Address distribution
	 */
	public Distribution<Address> getSelectDistribution();
	
	/**
	 * Execute the rate function for this Protocol instance.
	 * 
	 * @return
	 */
	public double getFrequency();

	/**
	 * Execute the update function of this protocol on another local protocol instance
	 * @param other
	 */
	public void executeUpdate(Protocol other);
	
	/** 
	 * Not currently used
	 * 
	 * @author lonnie
	 *
	 */
	public static enum Direction {
		PUSH, PULL, PUSHPULL
	};
	
	/**
	 * Returns an object that represents local node state.  By default, the instance itself.
	 * This will be JSON-serialized into the log.  Circular references cause serialization to break;
	 * in that case, this function will need to be overridden or the offending fields marked as transient.
	 * @return JSON-serializable object representing node state for logging purposes
	 */
	public Object getLogState();
}
