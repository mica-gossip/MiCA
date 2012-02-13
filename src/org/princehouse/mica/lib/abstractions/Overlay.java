package org.princehouse.mica.lib.abstractions;


import java.util.Collection;

import org.princehouse.mica.base.net.model.Address;


/**
 * An overlay is an object that exports a set of addresses for uniform random gossip
 * 
 * TODO: implement a distribution-based overlay 
 * 
 * @author lonnie
 *
 */
public interface Overlay {
	
	public Collection<Address> getView();
}
