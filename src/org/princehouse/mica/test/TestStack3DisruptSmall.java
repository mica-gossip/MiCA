package org.princehouse.mica.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTree;
import org.princehouse.mica.lib.TreeCountNodes;
import org.princehouse.mica.lib.TreeLabelNodes;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
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
public class TestStack3DisruptSmall extends TestHarness<MergeIndependent> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args)   {


		F3<Integer, Address, List<Address>, MergeIndependent> createNodeFunc = new F3<Integer, Address, List<Address>, MergeIndependent>() {
			@Override
			public MergeIndependent f(Integer i, Address address,
					List<Address> neighbors) {

				Overlay view = new StaticOverlay(neighbors);

				MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
				leaderElection.setName(String.format("leader-%d",i));

				SpanningTree tree = new SpanningTree(leaderElection,view);
				tree.setName(String.format("tree-%d",i));

				TreeCountNodes counting = new TreeCountNodes(tree);
				counting.setName(String.format("count-%d",i));

				TreeLabelNodes labeling = new TreeLabelNodes(tree,counting);
				labeling.setName(String.format("label-%d",i));

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


		final TestStack3DisruptSmall harness = new TestStack3DisruptSmall();
		harness.addTimer(10*60*1000, harness.taskStop());

		for(int i = 0; i < 3; i++) {
			TimerTask disrupt = new TimerTask() {
				@Override
				public void run() {
					Runtime.debug.println("----> Leader sabotage!");
					Runtime.log("-,-,-,artificial_disruption,leader_sabotage");
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
			harness.addTimer((300+10*i)*1000, disrupt);
		}	
		// disrupt at 150, 160, 170, 180 seconds by setting all leaders to random valid addresses
		harness.runRandomGraph(0, 25, 4, createNodeFunc);
	}

}
