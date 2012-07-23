package org.princehouse.mica.lib.abstractions;

public enum MergeSelectionCase {
	NA, //no selection has yet been made this round
	P1, // gossip only p1
	P2, // gossip only p2
	BOTH,  // gossip both
	NEITHER // do not gossip
}
