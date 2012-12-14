package org.princehouse.mica.test;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.ViewUniformRandom;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

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
	
	@ViewUniformRandom
	public Overlay view;
	
	public TestLoggingRemoteLocal(Overlay view) {
		super();
		this.view = view;
	}
	
	@GossipUpdate
	@Override
	public void update(Protocol p) {
		TestLoggingRemoteLocal that = (TestLoggingRemoteLocal) p;
		this.logJson(LogFlag.user, "log-this");
		that.logJson(LogFlag.user, "log-that");
	}
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, createNodeFunc);
	}
	
	public static F3<Integer, Address, Overlay, Protocol> createNodeFunc = new F3<Integer, Address, Overlay, Protocol>() {
		@Override
		public Protocol f(Integer i, Address address,
				Overlay neighbors) {
			// Create a static overlay to bootstrap our set of neighbors
			return new TestLoggingRemoteLocal(neighbors);
		}
	};
	
}
