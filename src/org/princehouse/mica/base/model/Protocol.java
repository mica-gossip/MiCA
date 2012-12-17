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

	
	public Address getAddress();
	
	/** 
	 * Not currently used
	 * 
	 * @author lonnie
	 *
	 */
	public static enum Direction {
		PUSH(true,false), PULL(false,true), PUSHPULL(true,true);
		
		private boolean doesPush;
		private boolean doesPull;
		private Direction(boolean push, boolean pull) {
			doesPush = push;
			doesPull = pull;
		}
		public boolean push() {
			return doesPush;
		}
		public boolean pull() {
			return doesPull;
		}
	};
	
	/**
	 * Called by the runtime on the gossip initiator immediately before running the gossip update.
	 * 
	 * selected denotes the result of the select function.  It may be null. 
	 */
	public void preUpdate(Address selected);


	/**
	 * Called when we fail to establish a connection with a peer for unknown reasons 
	 * @param selected
	 */
	public void unreachable(Address selected);
	
	/**
	 * Called when we establish a connection, but the peer we reach replies that it is too busy
	 * @param selected
	 */
	public void busy(Address selected);
	
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

	void logJson(Object flags, String eventType);

	void logJson(Object flags, String eventType, Object obj);

}
