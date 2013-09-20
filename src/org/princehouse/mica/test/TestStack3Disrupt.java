package org.princehouse.mica.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.example.TreeLabelNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.Merge;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.harness.TestHarness;

import fj.F3;

/**
 * Tests leader election + spanning tree + counting + labeling
 * 
 * @author lonnie
 * 
 */
public class TestStack3Disrupt extends TestHarness {

    /**
     * @param args
     * @throws UnknownHostException
     */
    public static void main(String[] args) {

        F3<Integer, Address, Overlay, Protocol> createNodeFunc = new F3<Integer, Address, Overlay, Protocol>() {
            @Override
            public Protocol f(Integer i, Address address, Overlay view) {

                MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
                SpanningTreeOverlay tree = new SpanningTreeOverlay(leaderElection, view);
                TreeCountNodes counting = new TreeCountNodes(tree);
                TreeLabelNodes labeling = new TreeLabelNodes(counting);

                return MergeIndependent.merge(MergeIndependent.merge(leaderElection, labeling),
                        MergeIndependent.merge(tree, counting));
            }
        };

        final TestStack3Disrupt harness = new TestStack3Disrupt();
        harness.addTimer(20 * 60 * 1000, harness.taskStop());

        for (int i = 0; i < 6; i++) {
            TimerTask disrupt = new TimerTask() {
                @Override
                public void run() {
                    MicaRuntime.debug.println("----> Leader sabotage!");
                    List<Address> addresses = new ArrayList<Address>();
                    for (MicaRuntime rt : harness.getRuntimes()) {
                        addresses.add(rt.getAddress());
                    }
                    for (MicaRuntime rt : harness.getRuntimes()) {
                        MergeIndependent temp = (MergeIndependent) ((Merge) rt.getProtocolInstance()).getP1();
                        MinAddressLeaderElection leader = (MinAddressLeaderElection) temp.getP1();
                        leader.setLeader(Randomness.choose(addresses));
                    }
                }
            };

            harness.addTimer((600 + 10 * i) * 1000, disrupt);
        }
        // disrupt at 150, 160, 170, 180 seconds by setting all leaders to
        // random valid addresses
        harness.runMain(args, createNodeFunc);
    }

}
