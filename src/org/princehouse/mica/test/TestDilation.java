package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.example.RoundRobinMerge;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;

import fj.F3;

/**
 * Tests leader election + spanning tree
 * 
 * @author lonnie
 * 
 */
public class TestDilation extends TestHarness<RoundRobinMerge> implements
		ProtocolInstanceFactory<RoundRobinMerge> {

	/**
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) {

		F3<Integer, Address, Overlay, MergeIndependent> createTestFunc = new F3<Integer, Address, Overlay, MergeIndependent>() {
			@Override
			public MergeIndependent f(Integer i, Address address, Overlay view) {

				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(
						view);

				Protocol tree = new SpanningTreeOverlay(leaderElection, view);

				return new MergeIndependent(leaderElection, tree);
			}
		};

		new TestDilation().runMain(args);
	}

	@Override
	public RoundRobinMerge createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {

		ProtocolInstanceFactory<Protocol> subfactory = new ProtocolInstanceFactory<Protocol>() {
			@Override
			public MergeIndependent createProtocolInstance(int nodeId,
					Address address, Overlay overlay) {
				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(
						overlay);
				Protocol tree = new SpanningTreeOverlay(leaderElection, overlay);
				return new MergeIndependent(leaderElection, tree);
			}
		};
		
		Protocol p1 = subfactory.createProtocolInstance(nodeId, address, overlay);
		Protocol p2 = subfactory.createProtocolInstance(nodeId, address, overlay);

		p2 = new Dilator(2,p2);
		
		return new RoundRobinMerge(p1,p2);
	}

}
