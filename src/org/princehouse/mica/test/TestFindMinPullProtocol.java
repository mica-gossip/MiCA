package org.princehouse.mica.test;


import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.example.FindMinPull;
import org.princehouse.mica.lib.abstractions.StaticOverlay;


public class TestFindMinPullProtocol {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub

		Address a1 = TCPAddress.valueOf("localhost:8001");
		Address a2 = TCPAddress.valueOf("localhost:8002");
		
		FindMinPull node1 = new FindMinPull(5, new StaticOverlay(a2));
		FindMinPull node2 = new FindMinPull(3, new StaticOverlay(a1));

		
		Runtime<FindMinPull> rt1 = SimpleRuntime.launchDaemon(node1, a1);
		Runtime<FindMinPull> rt2 = SimpleRuntime.launchDaemon(node2, a2);

		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		rt1.stop();
		rt2.stop();
		System.out.println("done");
	}

}
