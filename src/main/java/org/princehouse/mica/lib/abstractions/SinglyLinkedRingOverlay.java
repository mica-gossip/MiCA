package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.net.model.Address;

public interface SinglyLinkedRingOverlay extends Overlay {

  public Address getSuccessor();
}
