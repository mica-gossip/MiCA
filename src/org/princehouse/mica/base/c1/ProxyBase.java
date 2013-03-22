package org.princehouse.mica.base.c1;

import java.io.Serializable;
import org.princehouse.mica.base.model.Protocol;

public abstract class ProxyBase implements Serializable {

	private static final long serialVersionUID = 1L;

	// if true, this object represents a proxied remote object
	public boolean isProxy;
	
	// if not a proxy, serve as a wrapper
	public Object target;

	// constructor with no argument creates a proxy
	public ProxyBase() {
		isProxy = true;
		target = null;
	}
	
	
	public void setTarget(Object target) {
		this.target = target;
		isProxy = false;
	}

	/**
	 * Transition a wrapped object back into the object itself.  With throw an exception if called on an actual proxy
	 * @return
	 */
	public Object unbox() {
		if(isProxy) {
			throw new RuntimeException("cannot unbox an actual proxy");
		} else {
			return target;
		}
	}
	
	// package a target for proxying. this function will be implemented by the proxy class generator
	public abstract void box(Object target); 
	
	
	// copy changed values back to the original object. this function will be implemented by the proxy class generator
	// TODO make this abstract and implement in the proxy generator
	public void applyDiff(Object target) {}

	// execute update with this proxy as protocol a
	// TODO make this abstract and implement in the proxy generator
	public void executeUpdate(Protocol b) {}
}
