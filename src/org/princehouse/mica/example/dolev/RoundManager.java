package org.princehouse.mica.example.dolev;

import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;

public class RoundManager extends BaseProtocol {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private int f = 0;

    public int getF() {
        return f;
    }

    public void setF(int f) {
        this.f = f;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    private Set<Address> reached = Functional.set();

    @View
    public Overlay overlay = null;

    // number of nodes in network
    private int n;

    private int round = 0;

    public RoundManager(Overlay overlay, int n, int f) {
        this.f = f;
        this.n = n;
        this.overlay = overlay;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    @GossipUpdate
    @Override
    public void update(Protocol other) {
        RoundManager that = (RoundManager) other;
        reached.add(that.getAddress());
        that.reached.add(this.getAddress());
    }

    public boolean ready() {
        return getRemainingCount() <= 0;
    }

    public int getRemainingCount() {
        return (n - f) - reached.size();
    }

    public void reset() {
        reached.clear();
        setRound(getRound() + 1);
    }

}
