package org.princehouse.mica.example;

import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.Overlay;

/**
 * This is a merged four-protocol, self-stabilizing stack that sits on top of an
 * arbitrary overlay, "view" The main method demonstrates running an experiment
 * with this protocol using the TestHarness class to create many simulated nodes
 * on a random graph
 * 
 * Layer 1: Leader election. Nodes all agree on the same leader
 * 
 * Layer 2: Tree overlay builder. Uses the elected leader as the root of a tree
 * 
 * Layer 3: Subtree node count. Counts the nodes in each subtree.
 * 
 * Layer 4: Node labeling. Gives nodes unique label in DFS traversal order of
 * the tree. Uses the subtree node count.
 * 
 * Protocol includes a main() method that can be used to run an experiment on
 * the local machine. See TestHarness.TestHarnessOptions for command line
 * options.
 * 
 * The test harness will generate a log file named log.csv
 * 
 * @author lonnie
 * 
 */
public class FourLayerTreeStack extends MergeCorrelated {
    private static final long serialVersionUID = 1L;

    // Four sub-protocols
    private MinAddressLeaderElection leaderElection;
    private SpanningTreeOverlay tree;
    private TreeCountNodes counting;
    private TreeLabelNodes labeling;

    /**
     * Initialize a demo protocol instance. Bootstrap parameters are a view and
     * a unique node id used for logging.
     * 
     * @param view
     *            An overlay instance. StaticOverlay can be used to run on a
     *            fixed network graph.
     * @param nodeID
     *            A unique id used for logging.
     */
    public FourLayerTreeStack(Overlay view, int nodeID) {
        super();

        // Instantiate the four sub-protocols. setName() is optional, but having
        // named protocols makes the
        // logs easier to read
        leaderElection = new MinAddressLeaderElection(view);

        tree = new SpanningTreeOverlay(leaderElection, view);

        counting = new TreeCountNodes(tree);

        labeling = new TreeLabelNodes(counting);

        // The Merge operator merges two protocols, in order to accomodate four
        // we merge pairs into
        // merged sub-protocols.
        setP1(MergeCorrelated.merge(leaderElection, labeling)); // set merged
                                                                // protocol 1
        setP2(MergeCorrelated.merge(tree, counting)); // set merged protocol 2
    }

}
