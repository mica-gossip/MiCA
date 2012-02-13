package org.princehouse.mica.base.net.model;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface representing the general concept of an Address.
 * See TCPAddress for a TCP/IP specific address.
 * 
 * @author lonnie
 *
 */
public interface Address extends Serializable, Comparable<Address> {

	/**
	 * If address is local, open it for listening.
	 * Throw an AlreadyBoundException if address is already bound
	 * 
	 * An address may only 
	 * @param h
	 */
	public void bind(AcceptConnectionHandler h) throws IOException;
	
	/**
	 * If bound, unbind (network may close the connection; unspecified)
	 * If unbound, throw NotBoundException
	 */
	void unbind() throws NotBoundException; 

	/**
	 * Open a connection to this address.
	 * 
	 * @return Returns a new connection object
	 * @throws IOException If an error occurs
	 */
	public Connection openConnection() throws IOException;
	
	public String toString();

}