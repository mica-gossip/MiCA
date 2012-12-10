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
	 * @throws  
	 */
	public Distribution<Address> getView();
	
	/**
	 * Execute the rate function for this Protocol instance.
	 * 
	 * @return
	 */
	public double getRate();

	
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
	 * Called by the runtime on the gossip initiator immediately before running the gossip update.
	 * 
	 * selected denotes the result of the select function.  It may be null. 
	 */
	public void preUpdate(Address selected);


	/**
	 * Called by the runtime on the gossip initiator immediately after the update is executed 
	 * This is called even if the update method terminated with an exception or connection failure. I
	 * t can be thought of as a "finally" block for update
	 */
	public void postUpdate();
	
	
	/**
	 * The update function
	 * 
	 * @param that
	 */
	public void update(Protocol that);
	
	/**
	 * Called 
	 */
	
	/**
	 * Returns an object that represents local node state.  By default, the instance itself.
	 * This will be JSON-serialized into the log.  Circular references cause serialization to break;
	 * in that case, this function will need to be overridden or the offending fields marked as transient.
	 * @return JSON-serializable object representing node state for logging purposes
	 */
	public Object getLogState();
}
