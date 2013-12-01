package org.princehouse.mica.lib.abstractions;

import java.util.Map;

import org.princehouse.mica.base.FailureDetector;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.util.Functional;

/**
 * Parametric aggregator class. This can be used to implement any protocol which
 * follows the pattern:
 * 
 * 1. Collect summaries from neighbors 2. Compute aggregate function of
 * collected summaries
 * 
 * The Aggregator class caches the last known summary from each neighbor. This
 * cache is updated with each gossip exchange, and the aggregator recomputes the
 * aggregate function.
 * 
 * @author lonnie
 * 
 * @param <AgClass>
 *            Protocol class implementing the aggregator (i.e., the class you
 *            are writing)
 * @param <Summary>
 * @param <Aggregate>
 */
public abstract class Aggregator<Summary, Aggregate> extends FailureDetector {
    public Aggregator() {
    }

    private static final long serialVersionUID = 1L;

    private Map<Address, Summary> summaries = null;
    private Protocol.Direction direction;

    /**
     * Aggregate constructor. The direction of aggregation is given by constants
     * from Protocol.Direction. PUSH : Push summary of initiating gossip node to
     * recipient; compute new remote aggregate PULL : Pull summary of recipient
     * to initiator; compute new local aggregate PUSHPULL: Both
     * 
     * @param overlay
     *            The overlay to gossip along.
     * @param direction
     *            PUSH, PULL, or PUSHPULL
     * @param initialAggregateValue
     */
    public Aggregator(Protocol.Direction direction) {
        initializeSummaries();
        this.direction = direction;
    }

    @Override
    public void failureDetected(Address peer) {
        if (getSummaries().containsKey(peer)) {
            getSummaries().remove(peer);
        }
    }

    /**
     * Creates a PUSHPULL aggregate with a null initial value.
     * 
     * @param overlay
     *            The overlay to gossip along.
     */
    // public Aggregator(Overlay overlay) {
    // this(overlay, Protocol.Direction.PUSHPULL, null);
    // }

    /**
     * Initialize the summary map
     */
    public void initializeSummaries() {
        summaries = Functional.map();
    }

    /**
     * Return the current aggregate value
     * 
     * @return
     */
    public abstract Aggregate getAggregate();

    @Override
    public void preUpdate(Address selected) {
        super.preUpdate(selected);
        filterSummaries();
    }

    /**
     * Gossip update function
     * 
     * @param neighbor
     */
    @GossipUpdate
    @Override
    public void update(Protocol that) {
        super.update(that);
        @SuppressWarnings("unchecked")
        Aggregator<Summary, Aggregate> neighbor = (Aggregator<Summary, Aggregate>) that;
        if (direction.pull()) {
            updatePull(neighbor);
        }
        if (direction.push()) {
            updatePush(neighbor);
        }
    }

    public void updatePull(Aggregator<Summary, Aggregate> neighbor) {
        summaries.put(neighbor.getAddress(), neighbor.getSummary());
    }

    public void updatePush(Aggregator<Summary, Aggregate> neighbor) {
        neighbor.updatePull(this);
    }

    /**
     * 
     * If summaryFilterKeep returns false, the specified summary will be purged
     * from the summary cache. Otherwise it is kept. Default behavior is to keep
     * everything.
     * 
     * @param addr
     * @param s
     * @return
     */
    public boolean summaryFilterKeep(Address addr, Summary s) {
        return getView().get(addr) > 0;
    }

    /**
     * Return a map of all cached summaries
     * 
     * @return
     */
    public Map<Address, Summary> getSummaries() {
        return summaries;
    }

    public void filterSummaries() {
        for (Address addr : Functional.list(summaries.keySet())) {
            Summary value = summaries.get(addr);
            if (!summaryFilterKeep(addr, value))
                summaries.remove(addr);
        }
    }

    /**
     * Compute the local summary
     * 
     * @return
     */
    public abstract Summary getSummary();

}
