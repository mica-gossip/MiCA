package org.princehouse.mica.example;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.util.Distribution;

public class RoundRobinMergeRate extends BaseProtocol {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  // number of times p1 has gossiped
  private int np1 = 0;

  // number of times p2 has gossiped
  private int np2 = 0;

  private Protocol p1 = null;
  private Protocol p2 = null;

  public RoundRobinMergeRate(Protocol p1, Protocol p2) {
    this.p1 = p1;
    this.p2 = p2;
  }

  @View
  public Distribution<Address> view() {
    if (gossipP1()) {
      return p1.getView();
    } else {
      return p2.getView();
    }
  }

  private boolean currentRoundGossipP1 = true;

  @Override
  public void preUpdate(Address a) {
    currentRoundGossipP1 = gossipP1();
    if (currentRoundGossipP1) {
      np1++;
      p1.preUpdate(a);
    } else {
      np2++;
      p2.preUpdate(a);
    }
  }

  @Override
  public void postUpdate() {
    if (currentRoundGossipP1) {
      p1.postUpdate();
    } else {
      p2.postUpdate();
    }
  }

  @GossipUpdate
  @Override
  public void update(Protocol other) {
    RoundRobinMergeRate that = (RoundRobinMergeRate) other;
    if (currentRoundGossipP1) {
      p1.update(that.p1);
    } else {
      p2.update(that.p2);
    }
  }

  private boolean gossipP1() {
    double r1 = p1.getRate();
    double r2 = p2.getRate();

    // avoid divide-by-zero
    if (r2 == 0) {
      return true;
    }
    if (np2 == 0) {
      return false;
    }

    double targetRatio = r1 / r2;
    double actualRatio = ((double) np1) / ((double) np2);

    return targetRatio > actualRatio;
  }

  @GossipRate
  public double rate() {
    double r1 = p1.getRate();
    double r2 = p2.getRate();
    return r1 + r2;
  }

}
