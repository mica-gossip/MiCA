package org.princehouse.mica.example;

import java.io.Serializable;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.TokenRing;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;


public class DemoTokenRing extends TestHarness<MergeCorrelated> implements ProtocolInstanceFactory<MergeCorrelated> {

	public static void main(String[] args) {

		new DemoTokenRing().runMain(args);
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
		
		SinglyLinkedRingOverlay ring = (SinglyLinkedRingOverlay) overlay;
		MinAddressLeaderElection leader = new MinAddressLeaderElection(overlay);		
		NetworkSizeCounter networkSize = new StaticNetworkSizeCounter(getOptions().n);
		TokenRing tokenRing = new ChattyTokenRing(ring, leader, networkSize, String.format("[Act: Node %s]",nodeId));
		return MergeCorrelated.merge(tokenRing,leader);
	}
} 

