package org.princehouse.mica.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.example.TreeLabelNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
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
public class TestStack3DisruptLargeCorrelated extends TestHarness<MergeIndependent> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	
	public static long roundsToMs(int rounds) {
		return rounds * SimpleRuntime.DEFAULT_INTERVAL;
	}
	
	public static void main(String[] args) {


		F3<Integer, Address, Overlay, MergeIndependent> createNodeFunc = new F3<Integer, Address, Overlay, MergeIndependent>() {
			@Override
			public MergeIndependent f(Integer i, Address address,
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


		final TestStack3DisruptLargeCorrelated harness = new TestStack3DisruptLargeCorrelated();
		
		SimpleRuntime.DEFAULT_INTERVAL = 3000;
		int totalRounds = 600;
		
		harness.addTimer(roundsToMs(totalRounds), harness.taskStop());

		for(int i = totalRounds/2; i < totalRounds/2+1; i+=1) {
			TimerTask disrupt = new TimerTask() {
				@Override
				public void run() {
					Runtime.debug.println("----> Leader sabotage!");
					List<Address> addresses = new ArrayList<Address>();
					for(Runtime<MergeIndependent> rt : harness.getRuntimes()) {
						addresses.add(rt.getAddress());
					}
					for(Runtime<MergeIndependent> rt : harness.getRuntimes()) {
						MergeIndependent temp = (MergeIndependent) rt.getProtocolInstance().getP1();
						MinAddressLeaderElection leader = (MinAddressLeaderElection) temp.getP1();
						leader.setLeader(Randomness.choose(addresses));
					}
				}
			};
			harness.addTimer(roundsToMs(i), disrupt);
		}	
		harness.runMain(args, createNodeFunc);
	}

}
