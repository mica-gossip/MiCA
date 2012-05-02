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
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.TestHarness;

import fj.F3;

/**
 * Tests leader election + spanning tree + counting + labeling
 * @author lonnie
 *
 */
public class TestStack3DisruptLargeIndependent extends TestHarness<MergeCorrelated> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	
	public static long roundsToMs(int rounds) {
		return rounds * SimpleRuntime.DEFAULT_INTERVAL;
	}
	
	public static void main(String[] args) {

		SimpleRuntime.DEFAULT_INTERVAL = 5000;

		F3<Integer, Address, List<Address>, MergeCorrelated> createNodeFunc = new F3<Integer, Address, List<Address>, MergeCorrelated>() {
			@Override
			public MergeCorrelated f(Integer i, Address address,
					List<Address> neighbors) {

				Overlay view = new StaticOverlay(neighbors);

				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
			
				SpanningTreeOverlay tree = new SpanningTreeOverlay(leaderElection,view);
			

				TreeCountNodes counting = new TreeCountNodes(tree);
				

				TreeLabelNodes labeling = new TreeLabelNodes(tree,counting);
				

				return MergeCorrelated.merge(
						MergeCorrelated.merge(
								leaderElection,
								labeling
								),
								MergeCorrelated.merge(
										tree,
										counting
										));
			}
		};


		final TestStack3DisruptLargeIndependent harness = new TestStack3DisruptLargeIndependent();
		harness.addTimer(roundsToMs(1000), harness.taskStop());

		for(int i = 500; i < 550; i+=5) {
			TimerTask disrupt = new TimerTask() {
				@Override
				public void run() {
					Runtime.debug.println("----> Leader sabotage!");
					List<Address> addresses = new ArrayList<Address>();
					for(Runtime<MergeCorrelated> rt : harness.getRuntimes()) {
						addresses.add(rt.getAddress());
					}
					for(Runtime<MergeCorrelated> rt : harness.getRuntimes()) {
						MergeCorrelated temp = (MergeCorrelated) rt.getProtocolInstance().getP1();
						MinAddressLeaderElection leader = (MinAddressLeaderElection) temp.getP1();
						leader.setLeader(Randomness.choose(addresses));
					}
				}
			};
			harness.addTimer(roundsToMs(i), disrupt);
		}	
		
		harness.runMain(args,createNodeFunc);
	}

}
