package org.princehouse.mica.example.ecoop2014;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

/**
 * Use the subtree node count protocol to assign unique numbers to every node in
 * DFS order.
 * 
 * @author lonnie
 * 
 */
public class TreeLabelNodes extends BaseProtocol {

    public TreeLabelNodes() {
    }

    private static final long serialVersionUID = 1L;

    private TreeCountNodes count;

    public Distribution<Address> getView() {
        return count.getView();
    }

    private int label = 1;

    public TreeLabelNodes(TreeCountNodes count) {
        this.count = count;
    }

    public int getLabel() {
        return label;
    }

    private Map<Address, Integer> getChildLabelMap() {
        Map<Address, Integer> mp = Functional.map();
        List<Address> children = Functional.list(count.getTree().getChildren());
        Collections.sort(children);
        Map<Address, Integer> subtreeSizes = count.getSummaries();
        int i = label + 1;
        for (Address child : children) {
            Integer childSubtreeSize = subtreeSizes.get(child);
            if (childSubtreeSize == null) {
                childSubtreeSize = 1;
            }
            mp.put(child, i);
            i += childSubtreeSize;
        }
        return mp;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    @Override
    public void preUpdate(Address selected) {
        super.preUpdate(selected);
        if (count.getTree().isRoot()) {
            label = 1;
        }
    }

    @GossipUpdate
    @Override
    public void update(Protocol other) {
        TreeLabelNodes that = (TreeLabelNodes) other;
        Integer childLabel = getChildLabelMap().get(that.getAddress());
        if (childLabel == null) {
            childLabel = label + 1;
        }
        that.setLabel(childLabel);
    }

}
