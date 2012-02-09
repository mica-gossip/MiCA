package org.princehouse.mica.base;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.princehouse.mica.base.annotations.GossipFrequency;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.base.runtime.RuntimeState;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.MarkingObjectInputStream;


public abstract class BaseProtocol implements Protocol, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7604139731035133018L;
		
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

	public String toString() {
		return String.format("[%s@%s]", getName(), getRuntimeState().getAddress());  
	}
	
	@Override
	final public Distribution<Address> getSelectDistribution() {
		return Runtime.getRuntime().getSelectDistribution(this);
	}
	
	
	public Address getAddress() {
		return getRuntimeState().getAddress();
	}
	
	@Override
	public void executeUpdate(Protocol other) {
		Runtime.getRuntime().executeUpdate(this,other);
	}
	
	
	@Override 
	public double getFrequency() {
		return Runtime.getRuntime().getFrequency(this);
	}


	// Debugging functionality
	private String name;
	public String getName() {
		if(name == null) {
			setName(String.format("p%d",Runtime.getNewUID()));
		}
		return name;
	}
	public Protocol setName(String name) {
		this.name = name;
		return this;
	}
	public void log(String format, Object... arguments) {
		Runtime.log(String.format("%s,%s,%s,",getAddress(),getClass().getSimpleName(),getName()) + String.format(format, arguments));
	}
	public void logstate() {
		log("state,"+getStateString());
	}
	public String getStateString() {
		return "-";
	}
	
	@GossipFrequency
	public double frequency() {
		return 1.0;
	}
	
}
