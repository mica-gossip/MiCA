package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

import fj.F3;

/**
 * Tests leader election + spanning tree + counting
 * 
 * @author lonnie
 * 
 */
public class TestStack2 extends TestHarness {

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

                return MergeIndependent.merge(leaderElection, MergeIndependent.merge(tree, counting));
            }
        };

        new TestStack2().runMain(args, createNodeFunc);

    }

}
