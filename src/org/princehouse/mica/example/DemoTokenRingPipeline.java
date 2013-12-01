package org.princehouse.mica.example;

import java.io.Serializable;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.Pipeline;
import org.princehouse.mica.lib.TokenRing;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.NetworkSizeCounter;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;

/*
 * DemoTokenRingPipeline runs a demo of pipelining the token ring protocol along a static ring. 
 */
public class DemoTokenRingPipeline extends TestHarness implements ProtocolInstanceFactory {

    /**
     * Called by the test harness to generate a protocol instance (i.e.,
     * pipeline + leader election) for each node.
     */
    @Override
    public MergeCorrelated createProtocolInstance(final int nodeId, Address address, Overlay overlay) {

        // Number of nodes in system
        int n = getOptions().n;

        // Leader election protocol
        MinAddressLeaderElection leader = new MinAddressLeaderElection(overlay);

        // Pipeline running n copies of the ChattyTokenRing protocol
        Pipeline<TokenRing> pipeline = new Pipeline<TokenRing>(n, new TokenRingFactory(
                (SinglyLinkedRingOverlay) overlay, leader, n, nodeId));

        // Merge together the pipeline and leader election
        return MergeCorrelated.merge(leader, pipeline);
    }

    /**
     * Run pipeline example with command line arguments. See TestHarness class
     * for more details.
     * 
     * @param args
     */
    public static void main(String[] args) {
        new DemoTokenRingPipeline().runMain(args);
    }

    /**
     * Run the pipeline example with command line arguments. See TestHarness
     * class for more details.
     * 
     * @param args
     */
    public void runMain(String[] args) {
        runMain(args, this);
    }

    /**
     * Force the test harness to generate a ring overlay
     */
    @Override
    public void processOptions() {
        MicaOptions options = getOptions();
        options.graphType = "singlering";
        super.processOptions();
    }

    /**
     * This factory is used by the pipeline to create individual token ring
     * instances.
     * 
     * @author lonnie
     */
    public static class TokenRingFactory extends Pipeline.ProtocolFactory<TokenRing> implements Serializable {
        private static final long serialVersionUID = 1L;

        // Keep a "generation" counter for each pipeline, so we can watch
        // progress being
        // made as the debug messages scroll by.
        private int generation = 0;

        // Ring overlay to gossip along
        private SinglyLinkedRingOverlay ring = null;

        // Number of nodes in system
        private int n;

        // Id of the local node, for debugging
        private int nodeId;

        // Leader election protocol
        private LeaderElection leader;

        public TokenRingFactory(SinglyLinkedRingOverlay ring, LeaderElection leader, int n, int nodeId) {
            this.ring = ring;
            this.n = n;
            this.leader = leader;
            this.nodeId = nodeId;
        }

        @Override
        public TokenRing createProtocol() {
            NetworkSizeCounter networkSize = new DemoTokenRing.StaticNetworkSizeCounter(n);
            // Create the token ring instance
            TokenRing tokenRing = new DemoTokenRing.ChattyTokenRing(ring, leader, networkSize, String.format(
                    "[Act: Node %s gen %s]", nodeId, generation++));
            return tokenRing;
        }
    };
}
