package org.princehouse.mica.test;


import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.example.FindMinPush;
import org.princehouse.mica.util.Functional;


public class TestFindMinPushProtocol {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub

	
		Address a1 = TCPAddress.valueOf("localhost:8001");
		Address a2 = TCPAddress.valueOf("localhost:8002");
		
		FindMinPush node1 = new FindMinPush(5, Functional.set(a2));
		FindMinPush node2 = new FindMinPush(3, Functional.set(a1));

		
		Runtime<FindMinPush> rt1 = SimpleRuntime.launchDaemon(new SimpleRuntime<FindMinPush>(a1), node1);
		Runtime<FindMinPush> rt2 = SimpleRuntime.launchDaemon(new SimpleRuntime<FindMinPush>(a2), node2);

		
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
