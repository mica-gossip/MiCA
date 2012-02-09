package org.princehouse.mica.base.runtime;


import java.io.Serializable;
import java.util.Random;

import org.princehouse.mica.base.net.model.Address;


/**
 * This is the chunk of global state that Protocol instances have access to.
 * 
 * @author lonnie
 *
 */
public class RuntimeState implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RuntimeState() {
		random = new Random();
	}
	
	private Address address = null;
	
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		// long tid = Thread.currentThread().getId();
		// int hc = hashCode();
		// System.out.printf("setAddress. thread: %d     state-hash: %d    address: %s\n",tid,hc,address );
		
		if(this.address != null) 
			throw new RuntimeException("Address is read-only once it has been set"); 
		this.address = address;
	}
	public Random getRandom() {
		return random;
	}
	public void setRandom(Random random) {
		this.random = random;
	}
	private Random random; 
	
	public void update(RuntimeState update) {
		if(!update.getAddress().equals(getAddress())) {
			throw new RuntimeException("runtime state addresses differ!  should not change");
		}
		setRandom(update.getRandom());
	}
}
