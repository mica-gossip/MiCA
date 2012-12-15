package org.princehouse.mica.base.net.tcpip;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.net.BaseConnection;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;


/**
 * TCP/IP Address implementation.
 * 
 * See TCPAddress.valueOf to create a TCPAddress from a "host:port" string
 * 
 * @author lonnie
 *
 */
public class TCPAddress implements Address, Externalizable {
	
	/**
	 * Used when interpreting an address from a String if no port is specified
	 */
	public static final int DEFAULT_PORT = 8000; 
	
	transient private AcceptConnectionHandler receiveCallback;
	transient protected InetAddress address;
	transient protected ServerSocket sock;
	transient int port;
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof TCPAddress)) {
			return false;
		} else { 
			TCPAddress t = (TCPAddress) o;
			return address.equals(t.address) && port == t.port;
		}
		
	}
	
	/**
	 * Return the port associated with the address
	 * @return
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Get the IP address as a Java InetAddress
	 * @return An InetAddress that can be used with Java's built-in networking
	 */
	public InetAddress getInetAddressAddress() {
		return address;
	}
	
	/**
	 * Constructor from IP address and port number
	 *  
	 * @param address
	 * @param port
	 */
	public TCPAddress(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	@Override
	public void bind(AcceptConnectionHandler h) {
		receiveCallback = h;
		int backlog = 10;
		try {
			sock = new ServerSocket(port, backlog, address);
			sock.setReuseAddress(true);
		} catch(java.net.BindException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		AsyncServer.getServer().bind(this);
	}

	@Override
	public void unbind() {
		// not implemented.  haven't need it.
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d",address,port);
	}
	
	/** 
	 * Convert a "host:port" string into a TCPIPAddress instance
	 * 
	 * @param addr
	 * @return
	 * @throws UnknownHostException
	 */
	public static TCPAddress valueOf(String addr) throws UnknownHostException {
		int port;
		String host;
		
		if(addr.indexOf(':') >= 0) {			
			port = Integer.valueOf(addr.substring(addr.indexOf(':')+1)); 
			host = addr.substring(0,addr.indexOf(':'));
		} else {
			port = DEFAULT_PORT;
			host = addr;
		}
		return new TCPAddress(InetAddress.getByName(host), port);
	}

	protected void acceptCallback(Socket clientSocket) throws FatalErrorHalt, AbortRound {
		// clientSocket is returned from ServerSocket.accept
		assert(receiveCallback != null);
		
		try {
			Connection c = new SocketConnection(clientSocket);
			receiveCallback.acceptConnection(this,c);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Connection openConnection() throws IOException {
		Socket sock = new Socket(address, port);
		BaseConnection c = new SocketConnection(sock);
		return c;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		address = (InetAddress) in.readObject();
		port = (Integer) in.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(address);
		out.writeObject((Integer)port);
	}

	/**
	 * Default constructor
	 */
	public TCPAddress() {}


	public TCPAddress(String string) {
		try {
			TCPAddress temp = TCPAddress.valueOf(string);
			port = temp.port;
			address = temp.address;
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public int compareTo(Address o) {
		return toString().compareTo(o.toString());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
