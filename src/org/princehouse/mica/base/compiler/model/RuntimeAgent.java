package org.princehouse.mica.base.compiler.model;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.util.Distribution;

/**
 * Represents the "compiled" protocol.  
 * 
 *   A compiler takes a /protocol class/ and produces a /runtime agent/ 
 *   
 *   The runtime interacts with the agent and an instance of the protocol class
 *   
 * @author lonnie
 *
 * @param <P>
 */
public abstract class RuntimeAgent<P extends Protocol> {

	
	public RuntimeAgent() {}

	// Called on subprotocols as well as the top protocol; Runtime type parameter doesn't necessarily match pinstance type
	public abstract Address select(Runtime<?> rt, P pinstance, double nextDouble);

	// Called on subprotocols as well as the top protocol; Runtime type parameter doesn't necessarily match pinstance type
	public abstract Distribution<Address> getSelectDistribution(Runtime<?> rt, P protocol);

	// Only called on top protocol, so Runtime type parameter matches instance type
	public abstract void gossip(Runtime<P> rt, P pinstance, Connection connection);
	
	public abstract double getFrequency(Runtime<?> rt, P pinstance); 

}
