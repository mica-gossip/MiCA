package org.princehouse.mica.lib.abstractions;

import java.io.Serializable;
import java.util.List;

import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

public class RoundRobinOverlay implements Overlay, Serializable {
    private static final long serialVersionUID = 1L;

    private List<Address> sequence = null;

    public void setSequence(List<Address> sequence) {
        this.sequence = sequence;
    }

    public List<Address> getSequence() {
        return sequence;
    }

    public RoundRobinOverlay(List<Address> sequence) {
        setSequence(sequence);
    }

    @Override
    public Distribution<Address> getOverlay(RuntimeState rts) {
        if (sequence == null || sequence.size() == 0)
            return null;
        return Distribution.singleton(sequence.get(rts.getRound() % sequence.size()));
    }

}
