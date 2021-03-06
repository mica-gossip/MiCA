package org.princehouse.mica.example;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;

public class Dilator extends BaseProtocol {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private int dilationLevel = 1;

  @Override
  public void preUpdate(Address a) {
    p.preUpdate(a);
  }

  @Override
  public void postUpdate() {
    p.postUpdate();
  }

  @View
  public Protocol p = null;

  public static Dilator dilate(int level, Protocol p) {
    return new Dilator(level, p);
  }

  public Dilator(int level, Protocol p) {
    dilationLevel = level;
    this.p = p;
  }

  @GossipUpdate
  @Override
  public void update(Protocol other) {
    Dilator that = (Dilator) other;
    for (int i = 0; i < dilationLevel; i++) {
      RuntimeState rts = getRuntimeState();
      assert (rts != null);
      if (rts.getRandom().nextBoolean()) {
        return;
      }
    }
    p.update(that.p);
  }

  @GossipRate
  public double rate() {
    return p.getRate() * Math.pow(2, dilationLevel);
  }

}
