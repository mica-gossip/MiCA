package org.princehouse.mica.lib.abstractions;


import java.util.Collection;

import org.princehouse.mica.base.net.model.Address;


public interface Overlay {
	
	public Collection<Address> getView();
}
