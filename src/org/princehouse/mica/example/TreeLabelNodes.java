package org.princehouse.mica.example;


import java.util.Collections;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.RootedTree;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;


/**
 * Use the subtree node count protocol to assign unique numbers to every node in DFS order.
 * 
 * @author lonnie
 *
 */
public class TreeLabelNodes extends BaseProtocol {

	private static final long serialVersionUID = 1L;
	
	private RootedTree tree;
	private TreeCountNodes count;

	private int label = 1;

	public TreeLabelNodes(RootedTree t, TreeCountNodes count) {
		this.tree = t;
		this.count = count;
	}

	public int getLabel() {
		return label;
	}
	
	public List<Address> getChildren() {
		// sorted by address
		List<Address> temp = Functional.extend(Functional.<Address>list(),tree.getChildren());
		Collections.sort(temp);
		return temp;
	}
	
	@View
	public Distribution<Address> select() {
		return Distribution.uniform(getChildren());
	}
	
	public void setLabel(int label) {
		this.label = label;
	}
	
	@GossipUpdate 
	@Override
	public void update(Protocol that) {
		TreeLabelNodes child = (TreeLabelNodes) that;
		if(tree.isRoot()) {
			label = 1;
		}

		List<Address> children = getChildren(); // sorted in a total order
			
		int clabel = label + 1;	
		for(Address c : children) {
			if(c.equals(child.getAddress())) {
				child.setLabel(clabel);
				return;
			} else {
				clabel += count.getChildSubtreeSize(child.getAddress());
			}
		}
	} 

	
}
