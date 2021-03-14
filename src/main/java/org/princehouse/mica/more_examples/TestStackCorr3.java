package org.princehouse.mica.more_examples;

import fj.F3;
import java.net.UnknownHostException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.example.TreeLabelNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

/**
 * Tests leader election + spanning tree + counting + labeling
 *
 * @author lonnie
 */
public class TestStackCorr3 extends TestHarness {

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

        return MergeCorrelated.merge(MergeCorrelated.merge(leaderElection, labeling),
            MergeCorrelated.merge(tree, counting));
      }
    };

    new TestStackCorr3().runMain(args, createNodeFunc);

  }

}
