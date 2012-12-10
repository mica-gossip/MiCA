package org.princehouse.ficus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

public class FicusQueryInstaller extends BaseProtocol {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@View
	public Overlay baseView;

	
	
	// integer is queryid
	public Map<Query,Distribution<Address>> routingTable = new HashMap<Query,Distribution<Address>>();


	public FicusQueryInstaller(Overlay view) {
		this.baseView = view;
	}

	public Set<Query> installedQueries = new HashSet<Query>();

	/**
	 * Called by the client to subscribe locally to a new query
	 * @param q
	 */
	public void installQuery(Query q) {
		installedQueries.add(q);
	}

	protected List<QueryData> dataQueue = new LinkedList<QueryData>();
	
	private void generateSourceData() {
		for(Query q : installedQueries) {
			if(q.isSource(this)) {
				dataQueue.add(q.generateSourceData(this));
			}
		}
	}

	@GossipUpdate
	@Override
	public void update(Protocol other) {
		FicusQueryInstaller that = (FicusQueryInstaller) other;
		generateSourceData();
		relayNewQueries(that);
		
		// TODO incomplete
		//for(Query q : installedQueries) {
		//}
	}

	
	private void relayNewQueries(FicusQueryInstaller that) {
		// Do we have any installed queries that /that/ is unaware of?
		for(Query q : Functional.<Query>setDifference(installedQueries, that.installedQueries)) {
			that.installQuery(q);
		}
	}		


}
