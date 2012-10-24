package org.princehouse.mica.base.model;


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
	
	/**
	 * Local round counter, represents how many times this runtime has gossiped
	 */
	private int round = 0;
	
	private Address address = null;
	
	public Address getAddress() {
		return address;
	}
	
	public int getRound() {
		return round;
	}
	public void setRound(int round) {
		this.round = round;
	}
	
	public void incrementRound() {
		setRound(getRound()+1);
	}
	
	public void setAddress(Address address) {
		// long tid = Thread.currentThread().getId();
		// int hc = hashCode();
		// System.out.printf("setAddress. thread: %d     state-hash: %d    address: %s\n",tid,hc,address );
		
		if(this.address != null && !(this.address.equals(address)))
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
