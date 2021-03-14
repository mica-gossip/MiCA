package org.princehouse.mica.lib.abstractions;

public enum MergeSelectionCase {
  NA, // no selection has yet been made this round
  P1, // gossip only p1
  P2, // gossip only p2
  BOTH_P1P2, // gossip both, p1 first
  BOTH_P2P1, // gossip both, p2 first
  NEITHER // do not gossip
  ;

  public boolean p1Gossips() {
    return equals(P1) || bothGossip();
  }

  public boolean p2Gossips() {
    return equals(P2) || bothGossip();
  }

  public boolean bothGossip() {
    return equals(BOTH_P1P2) || equals(BOTH_P2P1);
  }

}
