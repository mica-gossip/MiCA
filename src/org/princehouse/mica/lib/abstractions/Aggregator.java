package org.princehouse.mica.lib.abstractions;

import java.util.Map;

import org.princehouse.mica.base.ExternalSelectProtocol;
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
public abstract class Aggregator<AgClass extends ExternalSelectProtocol, Summary, Aggregate>
		extends ExternalSelectProtocol {

	private static final long serialVersionUID = 1L;

	private Map<Address, Summary> summaries = null;
	private Protocol.Direction direction;

	private Aggregate aggregate = null;

	/**
	 * Aggregate constructor. The direction of aggregation is given by constants from Protocol.Direction.
	 *   PUSH : Push summary of initiating gossip node to recipient; compute new remote aggregate
	 *   PULL : Pull summary of recipient to initiator; compute new local aggregate
	 *   PUSHPULL: Both
	 *   
	 * @param overlay The overlay to gossip along.
	 * @param direction PUSH, PULL, or PUSHPULL
	 * @param defaultAggregateValue
	 */
	public Aggregator(Overlay overlay, Protocol.Direction direction, Aggregate defaultAggregateValue) {
		super(overlay);
		initializeSummaries();
		this.direction = direction;
		this.aggregate = defaultAggregateValue;
	}

	/**
	 * Creates a PUSHPULL aggregate with a null initial value.
	 * 
	 * @param overlay The overlay to gossip along.
	 */
	public Aggregator(Overlay overlay) {
		this(overlay, Protocol.Direction.PUSHPULL, null);
	}
	
	/**
	 * Initialize the summary map
	 */
	public void initializeSummaries() {
		summaries = Functional.map();
	}

	/**
	 * Return the current aggregate value
	 * @return
	 */
	public Aggregate getAggregate() {
		return aggregate;
	}

	/** 
	 * Set the current aggregate value
	 * @param a
	 */
	public void setAggregate(Aggregate a) {
		this.aggregate = a;
	}

	/**
	 * Gossip update function
	 * 
	 * @param neighbor
	 */
	@GossipUpdate
	@Override
	public void update(Protocol that) {
		@SuppressWarnings("unchecked")
		AgClass neighbor = (AgClass) that;
		if (direction == Protocol.Direction.PULL
				|| direction == Protocol.Direction.PUSHPULL) {
			updatePull(neighbor);
		}
		if (direction == Protocol.Direction.PUSH
				|| direction == Protocol.Direction.PUSHPULL) {
			updatePush(neighbor);
		}
	}

	public void updatePull(AgClass neighbor) {
		summaries.put(neighbor.getAddress(), computeSummary(neighbor));
		filterSummaries();
		setAggregate(computeAggregate(getSummaries()));
	}

	public void updatePush(AgClass neighbor) {
		// ugly casting :(
		@SuppressWarnings("unchecked")
		AgClass temp = (AgClass) this;
		@SuppressWarnings("unchecked")
		Aggregator<AgClass, Summary, Aggregate> neighbortemp = (Aggregator<AgClass, Summary, Aggregate>) neighbor;
		neighbortemp.updatePull(temp);
	}

	/**
	 * Compute an aggregate value from retained summaries
	 * 
	 * @param summaries
	 * @return
	 */
	public abstract Aggregate computeAggregate(Map<Address, Summary> summaries);

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
		return true;
	}

	/**
	 * Get the current summary for a given address
	 * @param addr Address
	 * @return Current summary for address, or getDefaultSummary(addr) if none known
	 */
	public Summary getSummary(Address addr) {
		if (summaries.containsKey(addr)) {
			return summaries.get(addr);
		} else {
			return getDefaultSummary(addr);
		}
	}

	/**
	 * Default summary: Used if the summary requested for an address is not cached.
	 * 
	 * @param addr
	 * @return
	 */
	public Summary getDefaultSummary(Address addr) {
		return null;
	}

	/**
	 * Return a map of all cached summaries
	 * 
	 * @return
	 */
	public Map<Address, Summary> getSummaries() {
		return summaries;
	}

	private void filterSummaries() {
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
	public abstract Summary computeSummary(AgClass neighbor);

}
