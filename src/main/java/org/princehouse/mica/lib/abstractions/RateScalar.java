package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.sugar.annotations.GossipRate;

/**
 * Scale the rate of a protocol
 *
 * @author lonnie
 */
public class RateScalar extends ProtocolAdapter {

  private static final long serialVersionUID = 1L;

  private double scalar;

  public double getScalar() {
    return scalar;
  }

  public void setScalar(double scalar) {
    this.scalar = scalar;
  }

  public RateScalar(BaseProtocol protocol, double scalar) {
    super(protocol);
    setScalar(scalar);
  }

  @GossipRate
  public double rate() {
    return super.rate() * scalar;
  }
}
