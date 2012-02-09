package org.princehouse.mica.lib;


import java.util.Map;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Aggregator;
import org.princehouse.mica.lib.abstractions.Tree;


public class TreeCountNodes extends Aggregator<TreeCountNodes, Integer, Integer> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Tree tree;
	
	public TreeCountNodes(Tree t) {
		super(t.getChildrenAsOverlay(), Protocol.Direction.PULL);
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
		return child.getSubtreeSize();
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
