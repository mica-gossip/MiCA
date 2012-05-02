package org.princehouse.mica.test;

import fj.F3;

import java.net.UnknownHostException;
import java.util.List;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.TestHarness;


/**
 * Tests leader election + spanning tree
 * @author lonnie
 *
 */
public class TestStack1 extends TestHarness<MergeIndependent> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) {
	
		
		F3<Integer, Address, List<Address>, MergeIndependent> createNodeFunc = new F3<Integer, Address, List<Address>, MergeIndependent>() {
			@Override
			public MergeIndependent f(Integer i, Address address,
					List<Address> neighbors) {
				
				Overlay view = new StaticOverlay(neighbors);
				
				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
				
				Protocol tree = new SpanningTreeOverlay(leaderElection,view);
				
				return new MergeIndependent(
						leaderElection,
						tree);
			}
		};

		new TestStack1().runMain(args, createNodeFunc);
		
	}

}
