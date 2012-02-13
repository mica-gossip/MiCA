package org.princehouse.mica.test;


import java.net.UnknownHostException;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.util.Functional;


/**
 * Duplicates runtime state address in local protocol state, and then makes sure they stay consistent
 * 
 * @author lonnie
 *
 */
public class TestRuntimeStateProtocol extends BaseProtocol {

	public Address a;

	@SelectUniformRandom
	public Set<Address> view; 

	public TestRuntimeStateProtocol(Address a, Set<Address> view) {
		this.a = a;
		this.view = view;
	}

	@GossipUpdate
	public void update(TestRuntimeStateProtocol other) {
		
		try {
		System.out.printf("this address test: (%s,%s)   other address test: (%s,%s)   %s\n",
				a, getRuntimeState().getAddress(),
				other.a, other.getRuntimeState().getAddress(),

				( a.equals(getRuntimeState().getAddress()) && 
						other.a.equals(other.getRuntimeState().getAddress()) &&
						!a.equals(other.a)) ? "OK" : "FAIL");

		}catch(Exception e) {
			System.out.printf("EXCEPTION %s\n", e);
		}
	}



	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub

		Address a1 = TCPAddress.valueOf("localhost:8001");
		Address a2 = TCPAddress.valueOf("localhost:8002");

		TestRuntimeStateProtocol node1 = new TestRuntimeStateProtocol(a1, Functional.set(a2));
		TestRuntimeStateProtocol node2 = new TestRuntimeStateProtocol(a2, Functional.set(a1));

		Runtime<TestRuntimeStateProtocol> rt1 = SimpleRuntime.launchDaemon(node1, a1);
		Runtime<TestRuntimeStateProtocol> rt2 = SimpleRuntime.launchDaemon(node2, a2);

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
