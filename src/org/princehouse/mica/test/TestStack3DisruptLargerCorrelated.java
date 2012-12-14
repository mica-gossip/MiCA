package org.princehouse.mica.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.example.TreeLabelNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeBase;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.harness.TestHarness;

import fj.F3;

/**
 * Tests leader election + spanning tree + counting + labeling
 * @author lonnie
 *
 */
public class TestStack3DisruptLargerCorrelated extends TestHarness {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	
	public static long roundsToMs(int rounds, int intervalMS) {
		return rounds * intervalMS;
	}
	
	public static void main(String[] args) {


		F3<Integer, Address, Overlay, Protocol> createNodeFunc = new F3<Integer, Address, Overlay, Protocol>() {
			@Override
			public Protocol f(Integer i, Address address,
					Overlay view) {

				

				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
				

				SpanningTreeOverlay tree = new SpanningTreeOverlay(leaderElection,view);

				TreeCountNodes counting = new TreeCountNodes(tree);
			
				TreeLabelNodes labeling = new TreeLabelNodes(tree,counting);

				return MergeIndependent.merge(
						MergeIndependent.merge(
								leaderElection,
								labeling
								),
								MergeIndependent.merge(
										tree,
										counting
										));
			}
		};


		final TestStack3DisruptLargerCorrelated harness = new TestStack3DisruptLargerCorrelated();
		
		int totalRounds = 600;
		
		harness.addTimer(roundsToMs(totalRounds, harness.getOptions().roundLength), harness.taskStop());

		for(int i = totalRounds/2; i < totalRounds/2+1; i+=1) {
			TimerTask disrupt = new TimerTask() {
				@Override
				public void run() {
					Runtime.debug.println("----> Leader sabotage!");
					List<Address> addresses = new ArrayList<Address>();
					for(Runtime rt : harness.getRuntimes()) {
						addresses.add(rt.getAddress());
					}
					for(Runtime rt : harness.getRuntimes()) {
						MergeIndependent temp = (MergeIndependent) ((MergeBase)rt.getProtocolInstance()).getP1();
						MinAddressLeaderElection leader = (MinAddressLeaderElection) temp.getP1();
						leader.setLeader(Randomness.choose(addresses));
					}
				}
			};
			harness.addTimer(roundsToMs(i, harness.getOptions().roundLength), disrupt);
		}	
		harness.runMain(args, createNodeFunc);
	}

}
