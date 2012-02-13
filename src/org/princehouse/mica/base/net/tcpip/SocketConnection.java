package org.princehouse.mica.base.net.tcpip;


import java.io.IOException;
import java.net.Socket;

import org.princehouse.mica.base.net.BaseConnection;


/**
 * TCP/IP implementation of Connection
 * 
 * @author lonnie
 *
 */
public class SocketConnection extends BaseConnection {

	private Socket sock;
	
	public SocketConnection(Socket sock) throws IOException {
		super(sock.getInputStream(), sock.getOutputStream());
		this.sock = sock;
	}
	
	@Override
	public void close() throws IOException {
		sock.close();
	}

}
