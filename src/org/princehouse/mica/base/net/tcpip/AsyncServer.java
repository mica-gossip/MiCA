package org.princehouse.mica.base.net.tcpip;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;

/**
 * TCP/IP communication facilitator
 * 
 * @author lonnie
 *
 */
public class AsyncServer {

	private static class SocketThread extends Thread {
		TCPAddress address;

		SocketThread(TCPAddress address) {
			this.address = address;
			setDaemon(true);
		}

		@Override
		public void run() {
			while(true) {
				try {
					Socket s = address.sock.accept();
					try {
						address.acceptCallback(s);
					} catch (FatalErrorHalt e) {
						break; // thread dies
					} catch (AbortRound e) {
						// ignore
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}


	}

	// Meant to be a singleton
	private static AsyncServer server;

	public static AsyncServer getServer() {
		if(server == null) {
			server = new AsyncServer();
		}
		return server;
	}

	private AsyncServer() {}

	private LinkedList<SocketThread> threads = new LinkedList<SocketThread>();

	public void bind(TCPAddress address) {
		SocketThread t = new SocketThread(address);
		t.start();
		threads.add(t);
	}

	

	public void stop() {
		while(threads.size() > 0) {
			System.out.printf("Stopping %d SocketThreads\n", threads.size());
			for(SocketThread t : threads) {
				t.interrupt();
				try {
					t.join(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
