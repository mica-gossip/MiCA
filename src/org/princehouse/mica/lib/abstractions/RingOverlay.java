package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.net.model.Address;


public interface RingOverlay extends Overlay {
	public Address getSuccessor();
}
