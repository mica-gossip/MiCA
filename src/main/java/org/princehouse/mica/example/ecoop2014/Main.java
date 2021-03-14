package org.princehouse.mica.example.ecoop2014;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

/**
 * Launches a simulated MiCA network to test the 
 * protocol stack: leader election + spanning tree + counting + labeling
 * 
 * See TestHarness.java for a list and description of possible command line
 * parameters for the test harness.
 * 
 */
public class Main extends TestHarness {

    /**
     * Create an instance of our protocol.  The view overlay is supplied
     * by the test harness, choosing from a number of possible static
     * topologies.
     * 
     * The parameter i is a unique id for this
     * protocol instance, and address is its designated address.
     */
    @Override
    public Protocol createProtocolInstance(int i, Address address, Overlay view) {
        MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(view);
        SpanningTreeOverlay tree = new SpanningTreeOverlay(leaderElection, view);
        TreeCountNodes counting = new TreeCountNodes(tree);
        TreeLabelNodes labeling = new TreeLabelNodes(counting);
        return MergeCorrelated.merge(MergeIndependent.merge(leaderElection, labeling),
                MergeCorrelated.merge(tree, counting));
    }

    public static void main(String[] args) {
        TestHarness test = new Main();
        test.runMain(args);
    }

}
