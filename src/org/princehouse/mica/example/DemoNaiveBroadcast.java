package org.princehouse.mica.example;

import java.util.TimerTask;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.NaiveBroadcast;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

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
public class DemoNaiveBroadcast extends TestHarness {
	
	
	public static class StringBroadcast extends NaiveBroadcast<String>  {

		private static final long serialVersionUID = 1L;

		public StringBroadcast(Overlay overlay) {
			super(overlay, 5);
		}
		
		@Override
		public void receiveMessage(String m) {
			logJson(LogFlag.user, "receive-broadcast-message", m);
		}
		
		
	};
	
	@Override
	public MicaOptions defaultOptions() {
		MicaOptions ops = super.defaultOptions();
		ops.implementation = "simple";
		return ops;
	}
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		final TestHarness harness = new DemoNaiveBroadcast();
		
		
		harness.addTimer(5000, new TimerTask() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				MicaRuntime rt = harness.getRuntimes().get(0);
				rt.getProtocolInstanceLock().lock();
				MiCA.getRuntimeInterface().getRuntimeContextManager().setNativeRuntime(rt);
				System.out.println("Sending message at 5 seconds");
				((NaiveBroadcast<String>)rt.getProtocolInstance()).sendMessage("hello world");
				rt.getProtocolInstanceLock().unlock();
			}
		}); 
		harness.runMain(args);
	}

	public Protocol createProtocolInstance(int i, Address address,
			Overlay view) {
		return  new StringBroadcast(view);
	}
	
}
