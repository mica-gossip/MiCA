package org.princehouse.mica.test;

import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.TestHarness;

import fj.F3;

/**
 * This is a merged four-protocol, self-stabilizing stack that sits on top of an
 * arbitrary overlay, "view" The main method demonstrates running an experiment
 * with this protocol using the TestHarness class to create many simulated nodes
 * on a random graph
 * 
 * Layer 1: Leader election. Nodes all agree on the same leader 
 * 
 * Layer 2: Tree overlay builder. Uses the elected leader as the root of a tree 
 * 
 * Layer 3: Subtree node count. Counts the nodes in each subtree. 
 * 
 * Layer 4: Node labeling.  Gives nodes unique label in DFS traversal order of the tree.
 *   Uses the subtree node count.
 * 
 * Protocol includes a main() method that can be used to run an experiment on the local machine.
 * See TestHarness.TestHarnessOptions for command line options.
 * 
 * The test harness will generate a log file named log.csv
 * 
 * @author lonnie
 * 
 */
public class TestLoggingRemoteLocal extends BaseProtocol {
	private static final long serialVersionUID = 1L;
	
	@SelectUniformRandom
	public Overlay view;
	
	public TestLoggingRemoteLocal(Overlay view) {
		super();
		this.view = view;
	}
	
	@GossipUpdate
	public void update(TestLoggingRemoteLocal that) {
		this.logJson("log-this");
		that.logJson("log-that");
	}
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, createNodeFunc);
	}

	
	public static F3<Integer, Address, List<Address>, TestLoggingRemoteLocal> createNodeFunc = new F3<Integer, Address, List<Address>, TestLoggingRemoteLocal>() {
		@Override
		public TestLoggingRemoteLocal f(Integer i, Address address,
				List<Address> neighbors) {
			// Create a static overlay to bootstrap our set of neighbors
			Overlay bootstrapView = new StaticOverlay(neighbors);
			return new TestLoggingRemoteLocal(bootstrapView);
		}
	};
	
}
