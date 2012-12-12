package org.princehouse.mica.example;

import java.util.TimerTask;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.NaiveBroadcast;
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
 * 
 * @author lonnie
 * 
 */
public class DemoNaiveBroadcast{
	
	
	public static class StringBroadcast extends NaiveBroadcast<String>  {

		private static final long serialVersionUID = 1L;

		public StringBroadcast(Overlay overlay) {
			super(overlay);
		}
		
		@Override
		public void receiveMessage(String m) {
			logJson(LogFlag.user, "receive-broadcast-message", m);
		}
		
		
	};
	
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		final TestHarness<StringBroadcast> harness = new TestHarness<StringBroadcast>();
		
		harness.addTimerRounds(5, new TimerTask() {
			@Override
			public void run() {
				Runtime<StringBroadcast> rt = harness.getRuntimes().get(0);
				rt.getProtocolInstanceLock().lock();
				rt.getProtocolInstance().sendMessage("hello world");
				rt.getProtocolInstanceLock().unlock();
			}
		});

		
		harness.runMain(args, createNodeFunc);
	}

	/**
	 * This createNodeFunc is used by the test harness to create individual node instances.
	 * 
	 * F3 is a fancy way of creating a callable object that can be passed 
	 * as a parameter to the test harness.  The f() function is what's important.
	 * 
	 * 
	 */
	public static F3<Integer, Address, Overlay, StringBroadcast> createNodeFunc = new F3<Integer, Address, Overlay, StringBroadcast>() {
		@Override
		public StringBroadcast f(Integer i, Address address,
				Overlay neighbors) {
			// Create a static overlay to bootstrap our set of neighbors
			StringBroadcast bc = new StringBroadcast(neighbors);
			return bc;
		}
	};
}
