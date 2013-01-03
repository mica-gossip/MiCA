package org.princehouse.mica.base.sim;

import java.io.IOException;

import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.net.model.NotBoundException;

public class SimAddress implements Address {

	private String id = null;
	
	protected String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	public SimAddress(String id) {
		this.id = id;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public int compareTo(Address a) {
		if(!(a instanceof SimAddress)) {
			return 0;
		} else {
			return getId().compareTo(((SimAddress)a).getId());
		}
	}

	@Override
	public void bind(AcceptConnectionHandler h) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unbind() throws NotBoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public Connection openConnection() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
