package org.princehouse.mica.example.syncpipe;

import java.util.Map;

import org.princehouse.mica.base.net.model.Address;

public interface Synchronizer {
	// returns a value that will be sent to the corresponding node
	public Object getValue();
	
	public void update(Replica r, Map<Address,Object> syncData);
}
