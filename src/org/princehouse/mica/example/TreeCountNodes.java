package org.princehouse.mica.example;


import java.util.Map;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Aggregator;
import org.princehouse.mica.lib.abstractions.RootedTree;


/**
 * Count the nodes in each subtree of a rooted spanning tree.  
 * Demonstration of the Aggregator class
 * 
 * @author lonnie
 *
 */
public class TreeCountNodes extends Aggregator<TreeCountNodes, Integer, Integer> {
	
	private static final long serialVersionUID = 1L;
	
	private RootedTree tree;
	
	public TreeCountNodes(RootedTree t) {
		super(t.getChildrenAsOverlay(), Protocol.Direction.PULL, 1);
		this.tree = t;
	}

	public int getChildSubtreeSize(Address child) {
		return getSummary(child);
	}
	
	public int subtreeSize = 1;
	
	// 1 + size of child subtrees
	@Override
	public Integer computeAggregate(Map<Address,Integer> childSizes) {
		int total = 1;
		for(int i : childSizes.values()) 
			total += i;
		return total;
	}
	
	public int getSubtreeSize() {
		return getAggregate();
	}
	
	
	@Override
	public String getStateString() {
		String tmp = "";
		for(Address c : tree.getChildren()) {
			tmp += String.format("%s=%s ",c, getSummary(c));
		}
		return String.format("subtree-size:%d   child subtrees: %s",getSubtreeSize(), tmp);
	}

	@Override
	public Integer computeSummary(TreeCountNodes child) {
		return getSubtreeSize();
	}
	
	@Override
	public Integer getDefaultSummary(Address address) {
		return 1;
	}
	
	@Override
	public boolean summaryFilterKeep(Address address, Integer value) {
		// keep if address is currently in our child list
		return tree.getChildren().contains(address);
	}
	
}
