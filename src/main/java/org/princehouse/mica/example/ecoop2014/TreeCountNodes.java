package org.princehouse.mica.example.ecoop2014;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.Aggregator;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.RootedTree;

/**
 * Count the nodes in each subtree of a rooted spanning tree. Demonstration of the Aggregator class
 *
 * @author lonnie
 */
public class TreeCountNodes extends Aggregator<Integer, Integer> {

  public TreeCountNodes() {
    super();
  }

  private static final long serialVersionUID = 1L;

  @View
  public Overlay overlay = null;

  private RootedTree tree = null;

  public RootedTree getTree() {
    return tree;
  }

  public void setTree(RootedTree tree) {
    this.tree = tree;
  }

  public TreeCountNodes(RootedTree tree) {
    super(Protocol.Direction.PULL);
    this.tree = tree;
    this.overlay = tree.getChildrenAsOverlay();
  }

  // 1 + size of child subtrees
  @Override
  public Integer getAggregate() {
    filterSummaries();

    int total = 1;
    for (int i : getSummaries().values()) {
      total += i;
    }
    aggregate = total; // persistent value only used for logging
    return total;
  }

  public int getSubtreeSize() {
    return getAggregate();
  }

  @Override
  public Integer getSummary() {
    return getSubtreeSize();
  }

  // this is just here so the aggregate is recorded in the logs... it's really
  // computed on-the-fly any time getAggregate is called
  public int aggregate = -23;

  @Override
  public void preUpdate(Address selected) {
    getAggregate();
  }

}
