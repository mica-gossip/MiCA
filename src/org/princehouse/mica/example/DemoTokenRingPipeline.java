package org.princehouse.mica.example;

import java.io.Serializable;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.Pipeline;
import org.princehouse.mica.lib.TokenRing;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;


public class DemoTokenRingPipeline extends TestHarness<MergeCorrelated> implements ProtocolInstanceFactory<MergeCorrelated> {

	public static void main(String[] args) {

		new DemoTokenRingPipeline().runMain(args);
	}

	public void runMain(String[] args) {
		runMain(args,this);
	}


	@Override
	public void processOptions() {
		TestHarness.TestHarnessOptions options = getOptions();
		options.graphType = "singlering";
		super.processOptions();
	}

	
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
	
	@Override
	public MergeCorrelated createProtocolInstance(final int nodeId, Address address,
			Overlay overlay) {
		int n = getOptions().n;
		MinAddressLeaderElection leader = new MinAddressLeaderElection(overlay);		
		Pipeline<TokenRing> pipeline = new Pipeline<TokenRing>(n, new PFactory( (SinglyLinkedRingOverlay) overlay, leader,  n, nodeId));
		
		return MergeCorrelated.merge(leader, pipeline);
	}
	
	public static class PFactory extends Pipeline.ProtocolFactory<TokenRing> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int generation = 0;
		private SinglyLinkedRingOverlay ring = null;
		private int n;
		private int nodeId;
		private LeaderElection leader;
		
		public PFactory(SinglyLinkedRingOverlay ring, LeaderElection leader, int n, int nodeId) {
			this.ring = ring;
			this.n = n;
			this.leader = leader;
			this.nodeId = nodeId;
		}
		
		@Override
		public TokenRing createProtocol() {
			NetworkSizeCounter networkSize = new StaticNetworkSizeCounter(n);
			TokenRing tokenRing = new ChattyTokenRing(ring, leader, networkSize, String.format("[Act: Node %s gen %s]",nodeId, generation++));
			//return MergeCorrelated.merge(tokenRing,leader);
			return tokenRing;
		}
	};
} 

