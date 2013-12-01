package org.princehouse.mica.util.harness;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;

/**
 * Implementations of this interface create protocol instances for the
 * TestHarness
 * 
 * @author lonnie
 * 
 * @param
 */
public interface ProtocolInstanceFactory {
    public Protocol createProtocolInstance(int nodeId, Address address, Overlay overlay);
}