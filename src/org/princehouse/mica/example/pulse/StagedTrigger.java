package org.princehouse.mica.example.pulse;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

/**
 *    incomplete!
 *    
 * MO: Attempt to communicate round-robin with all nodes in the supplied overlay view
 * 
 * 
 * 
 * @author lonnie
 *
 */
public class StagedTrigger extends BaseProtocol {
	private static final long serialVersionUID = 1L;
	
	private int i = 0; // cursor into view
	
	public Object view;
	
	public StagedTrigger(Object view) {
		this.view = view;
		
	}
	
	private List<Address> getSortedView() {
		Distribution<Address> dist;
		try {
			dist = Selector.asDistribution(view, getRuntimeState());
		} catch (SelectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Set<Address> keys = dist.keySet();
		List<Address> lst = Functional.list(keys);
		Collections.sort(lst);
		return lst;
	}
	
	
	@View
	public Address getNextComm() {
		List<Address> sview = getSortedView();
		int index = (i++) % sview.size();
		return sview.get(index);
	}
	
	@GossipUpdate
	public void update(StagedTrigger that) {
		// TODO NOT IMPLEMENTED
	}
	
	
}
