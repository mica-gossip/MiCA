package org.princehouse.mica.test;


import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.example.FindMinSymmetric;
import org.princehouse.mica.util.Functional;


public class TestFindMinSymmetricProtocolN {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub

		final int n = 100;
		
		Address addresses[] = new Address[n];
		
		for(int i = 0; i < n; i++) {
			addresses[i] = TCPAddress.valueOf(String.format("localhost:%d",8001+i));
		}

		List<Runtime<FindMinSymmetric>> rts = Functional.list();
		
		for(int i = 0; i < n; i++) {
			Set<Address> view = Functional.set();
			view.add(addresses[(i+1)%n]);
			view.add(addresses[(i+3)%n]);
			view.add(addresses[(i+n/2)%n]);
			FindMinSymmetric node = new FindMinSymmetric(i+1, view);
			rts.add(SimpleRuntime.launchDaemon(node,addresses[i]));
		}
		
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Runtime<FindMinSymmetric> rt : rts) {
			rt.stop();
		}
		System.out.println("done");
	}

}
