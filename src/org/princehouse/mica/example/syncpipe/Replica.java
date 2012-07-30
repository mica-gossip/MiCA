package org.princehouse.mica.example.syncpipe;

import java.util.Map;

import org.princehouse.mica.base.net.model.Address;

public interface Replica {

	// returns a value that will be sent to the corresponding node
	public Object getValue();

	/**
	 * Update the state of this replica, based on the states of replicas in corresponding positions from other nodes
	 * 
	 * @param i  Current position in the pipeline (0 = newest replica, k-1 = oldest replica)
	 * @param d  Data from other nodes' corresponding replicas.  The should be at least n-t entries.
	 */
	public void update(int i, Map<Address, Object> d);
}
