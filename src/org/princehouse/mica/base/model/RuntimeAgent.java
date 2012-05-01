package org.princehouse.mica.base.model;

import java.net.ConnectException;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;

/**
 * Represents the "compiled" protocol to the MiCA Runtime.
 * 
 * A Compiler instance takes a Protocol implementation class and emits a
 * RuntimeAgent for that class. RuntimeAgent knows how to execute the
 * distributed update function. Any analysis results from the original Protocol
 * class are stored in the RuntimeAgent.
 * 
 * @author lonnie
 * 
 * @param <P>
 */
public abstract class RuntimeAgent<P extends Protocol> {

	public RuntimeAgent() {
	}

	// TODO modify to take a Random instance instead of a pre-generated random double
	/**
	 * Executes the select function for the specified proto col instance and
	 * chooses an address from the resulting address distribution.
	 * 
	 * @param runtime Current Runtime
	 * @param pinstance Protocol instance
	 * @param randomValue A random double supplied by the runtime. select should be deterministic.
	 * 
	 * @return Address of the chosen gossip peer
	 * @throws SelectException 
	 */
	public abstract Address select(Runtime<?> runtime, P pinstance, double randomValue) throws SelectException;

	// Called on subprotocols as well as the top protocol; Runtime type
	// parameter doesn't necessarily match pinstance type
	/**
	 * Executes the select function for the specified protocol instance. Returns an
	 * address distribution.
	 * 
	 * This may be called for protocols that are not necessarily of class P.
	 * 
	 * @param runtime Current Runtime 
	 * @param pinstance Protocol instance
	 * @return
	 * @throws SelectException 
	 */
	public abstract Distribution<Address> getSelectDistribution(Runtime<?> runtime,
			P pinstance) throws SelectException;

	/**
	 * Execute the gossip update with a remote peer.
	 * 
	 * @param runtime Current runtime
	 * @param pinstance Protocol instance
	 * @param connection Open connection to the selected gossip peer
	 */
	public abstract void gossip(Runtime<P> runtime, P pinstance,
			Connection connection);

	
	/**
	 * Returns the "rate" of a protocol instance, i.e., the number of times  
	 * per "round" the protocol gossips.
	 * 
	 * @param runtime
	 * @param pinstance
	 * @return
	 */
	public abstract double getRate(Runtime<?> runtime, P pinstance);

	public abstract void handleNullSelect(Runtime<?> runtime, P pinstance);

	public abstract void handleConnectException(Runtime<?> runtime, P pinstance, Address partner, ConnectException ce);
	
}
