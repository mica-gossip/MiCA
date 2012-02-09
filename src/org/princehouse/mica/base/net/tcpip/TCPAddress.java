package org.princehouse.mica.base.net.tcpip;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.princehouse.mica.base.net.base.BaseConnection;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;



public class TCPAddress implements Address, Externalizable {
	
	public static final int DEFAULT_PORT = 8005; 
	
	private AcceptConnectionHandler receiveCallback;
	
	protected InetAddress address;
	protected ServerSocket sock;
	int port;
	
	public boolean equals(Object o) {
		if(!(o instanceof TCPAddress)) {
			return false;
		} else { 
			TCPAddress t = (TCPAddress) o;
			return address.equals(t.address) && port == t.port;
		}
		
	}
	
	public int getPort() {
		return port;
	}
	
	public InetAddress getInetAddressAddress() {
		return address;
	}
	
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
			System.err.printf("In use: %s port %s\n",address, port);
			throw new RuntimeException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("socket binding failed");
		}
		AsyncServer.getServer().bind(this);
	}

	@Override
	public void unbind() {
		// not implemented
	}
		
	public String toString() {
		return String.format("%s:%d",address,port);
	}
	
	/** 
	 * Convert a host:port string into a TCPIPAddress instance
	 * 
	 * @param addr
	 * @return
	 * @throws UnknownHostException
	 */
	public static TCPAddress valueOf(String addr) throws UnknownHostException {
		// TODO Auto-generated method stub
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

	protected void acceptCallback(Socket clientSocket) {
		// clientSocket is returned from ServerSocket.accept
		assert(receiveCallback != null);
		
		try {
			Connection c = new SocketConnection(clientSocket);
			receiveCallback.acceptConnection(this,c);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Connection openConnection() throws IOException {
		// TODO Auto-generated method stub
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

	public TCPAddress() {}

	@Override
	public int compareTo(Address o) {
		return toString().compareTo(o.toString());
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
}
