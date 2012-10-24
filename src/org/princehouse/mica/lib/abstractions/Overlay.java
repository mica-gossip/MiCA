package org.princehouse.mica.lib.abstractions;


import java.util.List;

import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;


/**
 * An overlay is an object that exports a set of addresses for uniform random gossip
 * 
 * TODO: implement a distribution-based overlay 
 * 
 * @author lonnie
 *
 */
public interface Overlay {
	
	public Distribution<Address> getOverlay(RuntimeState rts) throws SelectException;
	
}
