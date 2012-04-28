package org.princehouse.mica.test;

import fj.F3;

import java.net.UnknownHostException;
import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeGeneric;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.TestHarness;


/**
 * Tests leader election + spanning tree
 * @author lonnie
 *
 */
public class TestStack1GenericMerge extends TestHarness<MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay>> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) {
	
		
		F3<Integer, Address, List<Address>, MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay>> createNodeFunc = new F3<Integer, Address, List<Address>, MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay>>() {
			@Override
			public MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay> f(Integer i, Address address,
					List<Address> neighbors) {
				Overlay view = new StaticOverlay(neighbors);
				
				MinAddressLeaderElection p1 = new MinAddressLeaderElection(view);
				p1.setName(String.format("leader-%d",i));

				SpanningTreeOverlay p2 = new SpanningTreeOverlay(p1,view);
				p2.setName(String.format("tree-%d",i));
				MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay> node = new MergeGeneric<MinAddressLeaderElection,SpanningTreeOverlay>(p1,p2);						
				return node;
			}
		};

		new TestStack1GenericMerge().runMain(args, createNodeFunc);
		
	}

}
