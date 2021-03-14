package org.princehouse.mica.example.ecoop2014;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.ViewUniformRandom;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.Overlay;

/**
 * Basic leader election protocol.
 * <p>
 * Addresses have a unique sort order. The "least" address in the system is eventually agreed upon
 * as the leader.
 * <p>
 * Not robust if the leader leaves the network
 */
public class MinAddressLeaderElection extends BaseProtocol implements LeaderElection {

  public MinAddressLeaderElection() {
  }

  private static final long serialVersionUID = 1L;
  private Address leader;

  @ViewUniformRandom
  public Overlay overlay;

  public MinAddressLeaderElection(Overlay overlay) {
    this.overlay = overlay;
  }

  @Override
  public Address getLeader() {
    if (leader == null) {
      leader = getRuntimeState().getAddress();
    }
    ;
    return leader;
  }

  public void setLeader(Address a) {
    leader = a;
  }

  @Override
  public boolean isLeader() {
    return getLeader().equals(getRuntimeState().getAddress());
  }

  @GossipUpdate
  @Override
  public void update(Protocol that) {
    MinAddressLeaderElection other = (MinAddressLeaderElection) that;
    Address a = getLeader();
    Address b = other.getLeader();

    // the greatest address gets to be leader
    if (a.compareTo(b) > 0) {
      leader = b;
    } else {
      other.leader = a;
    }
  }

}
