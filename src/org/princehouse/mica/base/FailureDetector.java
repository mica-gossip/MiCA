package org.princehouse.mica.base;

import java.util.Map;
import java.util.Set;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

public abstract class FailureDetector extends BaseProtocol {

    private static final long serialVersionUID = 1L;

    // number of consecutive failures to tolerate
    private int failureThreshold = 3;

    public Map<Address, Integer> consecutiveFailureMap = Functional.map();
    public Set<Address> failed = Functional.set();

    @Override
    public void preUpdate(Address selected) {
        if (selected == null)
            return;
        Integer record = consecutiveFailureMap.get(selected);
        if (failed.contains(selected))
            return;
        if (record == null) {
            return;
        } else if (record > 0) {
            // our last contact with this node failed
            consecutiveFailureMap.put(selected, -record);
        } else if (record < 0) {
            // our last contact with this node succeeded; remove it from the map
            consecutiveFailureMap.remove(selected);
        }
    }

    @Override
    public void unreachable(Address peer) {
        if (failed.contains(peer))
            return;
        Integer consecutiveFailures = consecutiveFailureMap.get(peer);
        if (consecutiveFailures == null) {
            consecutiveFailureMap.put(peer, 1);
            return;
        }

        if (consecutiveFailures < 0) {
            consecutiveFailures = -consecutiveFailures;
        }

        // make positive and increment number of failures
        consecutiveFailures++;
        if (consecutiveFailures > failureThreshold) {
            consecutiveFailureMap.remove(peer);
            System.err.printf("-------------> %s belives %s has failed\n", getAddress(), peer);
            failed.add(peer);
            failureDetected(peer);
            logJson(LogFlag.detectFailure, "detect-failure", peer);
        } else {
            consecutiveFailureMap.put(peer, consecutiveFailures);
        }

    }

    public void failureDetected(Address peer) {
        // override to do something
    }

    public void resurrectionDetected(Address peer) {
        failed.remove(peer);
        System.err.printf("-------------> %s learns %s has been resurrected!\n", getAddress(), peer);
        logJson(LogFlag.detectFailure, "detect-resurrection", peer);
    }

    @Override
    public void update(Protocol that) {
        Address ta = that.getAddress();
        if (failed.contains(ta)) {
            resurrectionDetected(ta);
        }
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureLevel(int failureLevel) {
        this.failureThreshold = failureLevel;
    }
}
