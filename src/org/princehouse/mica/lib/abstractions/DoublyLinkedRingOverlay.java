package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.net.model.Address;

public interface DoublyLinkedRingOverlay extends SinglyLinkedRingOverlay {
	public Address getPredecessor();
}
