package org.princehouse.mica.example;

import java.io.Serializable;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.TokenRing;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;


/*
 *   Runs an example TokenRing.  The ChattyTokenRing class defines a TokenRing that causes a node to print a message
 *   to the console when it gets the token.  
 *   
 *   This demo uses the TestHarness class to generate a static ring overlay.  A better TokenRing example might use 
 *   a ring-building protocol.  The MinLeaderElection protocol is used for leader election, and this is merged with 
 *   the token ring such that the protocol being run is
 *   
 *     merge(leaderElection, tokenRing) 
 *   
 *   The DemoTokenRing class is a TestHarness that creates many local node instances and generates a ring.
 * 
 *   Running this class, one will see MiCA print mis-ordered "Act" messages for a few rounds, eventually converging 
 *   into an orderly cyclical count-down once the leader election has converged and up to k rounds elapse to stabilize 
 *   the token ring.  
 */
public class DemoTokenRing extends TestHarness implements ProtocolInstanceFactory {
	
	/**
	 *  Called by the test harness to create a protocol instance for each node
	 */
	@Override
	public MergeCorrelated createProtocolInstance(final int nodeId, Address address,
			Overlay overlay) {
		
		// A static ring overlay is provided by the test harness, but needs to be cast
		SinglyLinkedRingOverlay ring = (SinglyLinkedRingOverlay) overlay;
		
		// Create a leader election protocol
		MinAddressLeaderElection leader = new MinAddressLeaderElection(overlay);
		
		/* TokenRing needs an accurate notion of network size for correctness.  In this demo, network size is 
		* known ahead of time and is fixed, so we use StaticNetworkSizeCounter to feed network size into the 
		* token ring.  In a more advance scenario, the NetworkSizeCounter could be a separate protocol tasked with
		* approximating network size.\
		* 
		* The ring overlay and network size counter do not need to be merged because, being completely static, 
		* they are not protocols.
		*/
		NetworkSizeCounter networkSize = new StaticNetworkSizeCounter(getOptions().n);
		
		// Create a ChattyTokenRing instance, feeding it ring / leader / networkSize inputs, along with a triumphant
		// message to print when the token arrives.
		TokenRing tokenRing = new ChattyTokenRing(ring, leader, networkSize, String.format("I have the token! (Node %s)",nodeId));
		
		// Merge together the leader election and token ring
		return MergeCorrelated.merge(tokenRing,leader);
	}
	
	
	/**
	 * Run with TestHarness command line options.  See TestHarness class or details.
	 * @param args
	 */
	public static void main(String[] args) {
		new DemoTokenRing().runMain(args);
	}

	/**
	 * Run protocol with command line args
	 * @param args
	 */
	public void runMain(String[] args) {
		runMain(args,this);
	}

	/**
	 * Force the test harness to generate a ring overlay.
	 */
	@Override
	public void processOptions() {
		MicaOptions options = getOptions();
		options.graphType = "singlering";
		super.processOptions();
	}
	
	/**
	 * Override the act() function of TokenRing to print a message to the console when the token arrives.
	 * 
	 * @author lonnie
	 *
	 */
	public static class ChattyTokenRing extends TokenRing {
		private static final long serialVersionUID = 1L;
		private String msg;
		public ChattyTokenRing(SinglyLinkedRingOverlay overlay, LeaderElection leader, NetworkSizeCounter size, String msg) {
			super(overlay, leader, size);
			this.msg = msg;
		}
		@Override
		public void act() {
			System.out.println(msg);
		}
	}
	
	/**
	 * Static network size.
	 * 
	 * @author lonnie
	 *
	 */
	public static class StaticNetworkSizeCounter implements NetworkSizeCounter, Serializable {
		private static final long serialVersionUID = 1L;

		public StaticNetworkSizeCounter(int n) {
			this.n  = n;
		}
		
		private int n;
		
		@Override
		public int size() {
			return n;
		}			
	}
	
	
} 

