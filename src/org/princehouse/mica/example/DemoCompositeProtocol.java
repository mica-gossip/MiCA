package org.princehouse.mica.example;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTree;
import org.princehouse.mica.lib.TreeCountNodes;
import org.princehouse.mica.lib.TreeLabelNodes;
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
 * @author lonnie
 * 
 */
public class DemoCompositeProtocol extends MergeCorrelated {
	private static final long serialVersionUID = 1L;

	private Overlay view;
	
	// four sub-protocols
	private MinAddressLeaderElection leaderElection;
	private SpanningTree tree;
	private TreeCountNodes counting;
	private TreeLabelNodes labeling;
	
	/**
	 * 
	 * @param view An overlay instance.  StaticOverlay can be used to run on a fixed network graph.
	 * @param nodeID A unique id used for logging.
	 */
	public DemoCompositeProtocol(Overlay view, int nodeID) {
		super();

		this.view = view;
		
		// Instantiate the four sub-protocols.  setName() is optional, but having named protocols makes the 
		// logs easier to read
		leaderElection = new MinAddressLeaderElection(view);
		leaderElection.setName(String.format("leader-%d", nodeID));

		tree = new SpanningTree(leaderElection, view);
		tree.setName(String.format("tree-%d", nodeID));

		counting = new TreeCountNodes(tree);
		counting.setName(String.format("count-%d", nodeID));

		labeling = new TreeLabelNodes(tree, counting);
		labeling.setName(String.format("label-%d", nodeID));

		// The Merge operator merges two protocols, in order to accomodate four we merge pairs into 
		// merged sub-protocols.
		setP1(MergeCorrelated.merge(leaderElection, labeling));  // set merged protocol 1
		setP2(MergeCorrelated.merge(tree, counting));  // set merged protocol 2
	}
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, createNodeFunc);
	}

	/**
	 * This createNodeFunc is used by the test harness to create individual node instances.
	 * 
	 * F3 is a fancy way of creating a callable object that can be passed 
	 * as a parameter to the test harness.  The f() function is what's important.
	 * 
	 * 
	 */
	public static F3<Integer, Address, List<Address>, DemoCompositeProtocol> createNodeFunc = new F3<Integer, Address, List<Address>, DemoCompositeProtocol>() {
		@Override
		public DemoCompositeProtocol f(Integer i, Address address,
				List<Address> neighbors) {
			Overlay view = new StaticOverlay(neighbors);
			return new DemoCompositeProtocol(view, i);
		}
	};
}
