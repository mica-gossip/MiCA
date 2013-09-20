package org.princehouse.mica.lib;

import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

/**
 * FIXME not complete
 * 
 * @author lonnie
 * 
 */
public class RingOverlayBuilder extends BaseProtocol implements SinglyLinkedRingOverlay {

    private static final long serialVersionUID = 1L;

    public Set<Address> candidates;
    public Address succ;
    public Overlay underlay;

    public RingOverlayBuilder(Overlay underlay) {
        candidates = Functional.set();
        succ = getAddress();
        this.underlay = underlay;
    }

    @Override
    public Address getSuccessor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distribution<Address> getOverlay(RuntimeState rts) throws SelectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void update(Protocol that) {
        // TODO Auto-generated method stub
        // NOT IMPLEMENTED
    }

}
