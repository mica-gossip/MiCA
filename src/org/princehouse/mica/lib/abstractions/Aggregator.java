package org.princehouse.mica.lib.abstractions;


import java.util.Map;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;


public abstract class Aggregator<AgClass extends BaseProtocol, Summary, Aggregate> extends BaseProtocol {	

	private static final long serialVersionUID = 1L;

	private Map<Address,Summary> summaries = null;
	private Protocol.Direction direction;
	
	private Aggregate aggregate = null;
	
	@Select
	private Overlay overlay = null;
	
	public Overlay getOverlay() {
		return overlay;
	}

	public void setOverlay(Overlay overlay) {
		this.overlay = overlay;
	}

	public Aggregator(Overlay overlay, Protocol.Direction direction) {
		initializeSummaries();
		setOverlay(overlay);
		this.direction = direction;
	}
	
	public void initializeSummaries() {
		summaries = Functional.map();
	}
	
	public Aggregate getAggregate() {
		if(aggregate == null)
			aggregate = computeAggregate(getSummaries());
		return aggregate;
	}
	
	public void setAggregate(Aggregate a) {
		this.aggregate = a;
	}
	
	@GossipUpdate
	public void update(AgClass neighbor) {
		if(direction == Protocol.Direction.PULL || direction == Protocol.Direction.PUSHPULL) {
			updatePull(neighbor);
		}
		if(direction == Protocol.Direction.PUSH || direction == Protocol.Direction.PUSHPULL) {
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
		Aggregator<AgClass,Summary,Aggregate> neighbortemp = (Aggregator<AgClass,Summary,Aggregate>) neighbor;
		neighbortemp.updatePull(temp);
	}
	
	/**
	 * Compute an aggregate value from retained summaries
	 * 
	 * @param summaries
	 * @return
	 */
	public abstract Aggregate computeAggregate(Map<Address,Summary> summaries);
		
	/**
	 *
	 * If summaryFilterKeep returns false, the specified summary will be purged 
	 * from the summary cache.  Otherwise it is kept.  Default behavior is to keep everything.
	 * 
	 * @param addr
	 * @param s
	 * @return
	 */
	public boolean summaryFilterKeep(Address addr, Summary s) {
		return true;
	}
	
	public Summary getSummary(Address addr) {
		if(summaries.containsKey(addr)) {
			return summaries.get(addr);
		} else {
			return getDefaultSummary(addr);
		}
	}
	
	public Summary getDefaultSummary(Address addr) {
		return null;
	}
	
	public Map<Address,Summary> getSummaries() {
		return summaries;
	}
	
	public void filterSummaries() {
		for(Address addr : Functional.list(summaries.keySet())) {
			Summary value = summaries.get(addr);
			if(!summaryFilterKeep(addr, value)) 
				summaries.remove(addr);
		}
	}
	
	
	public abstract Summary computeSummary(AgClass neighbor);

	
}
