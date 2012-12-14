package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.princehouse.mica.base.model.MicaOptions;
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
	public MicaOptions defaultOptions() {
		MicaOptions options = super.defaultOptions();
		options.n = 50;
		options.graphType = "random";
		options.rdegree = 8;
		options.timeout = 4000;
		options.roundLength = 4000;
		options.stopAfter = 100;
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

		p2 = Dilator.dilate(4,p2);
	
		return new RoundRobinMerge(p1,p2);
	}

}