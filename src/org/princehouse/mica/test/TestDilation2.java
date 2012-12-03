package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.example.RoundRobinMerge;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;

/**
 * Tests leader election + spanning tree
 * 
 * @author lonnie
 * 
 */
public class TestDilation2 extends TestHarness<RoundRobinMerge> implements
		ProtocolInstanceFactory<RoundRobinMerge> {


	@Override
	public TestHarnessOptions defaultOptions() {
		TestHarnessOptions options = super.defaultOptions();
		options.n = 100;
		options.graphType = "complete";
		options.timeout = 5000;
		options.roundLength = 10000;
		return options;
	}
	
	/**
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) {
		new TestDilation2().runMain(args);
	}

	@Override
	public RoundRobinMerge createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {

		ProtocolInstanceFactory<Protocol> subfactory = new ProtocolInstanceFactory<Protocol>() {
			@Override
			public Protocol createProtocolInstance(int nodeId,
					Address address, Overlay overlay) {
				return new MinAddressLeaderElection(overlay);
				
			}
		};
		
		Protocol p1 = subfactory.createProtocolInstance(nodeId, address, overlay);
		Protocol p2 = subfactory.createProtocolInstance(nodeId, address, overlay);
		Protocol p3 = subfactory.createProtocolInstance(nodeId, address, overlay);
		Protocol p4 = subfactory.createProtocolInstance(nodeId, address, overlay);

		p2 = Dilator.dilate(2,p2);
		p3 = Dilator.dilate(3,p3);
		p4 = Dilator.dilate(4,p4);
		
		return new RoundRobinMerge(
				new RoundRobinMerge(p1,p2),
				new RoundRobinMerge(p3,p4));
	}

}
