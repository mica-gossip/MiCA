package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaException;
import org.princehouse.mica.base.net.model.Address;

public abstract class SimulatorEvent implements Comparable<SimulatorEvent> {

  public long t;

  private boolean cancelled = false;

  private Address src = null; // runtime address executing this event

  public Address getSrc() {
    return src;
  }

  public void setSrc(Address src) {
    this.src = src;
  }

  public Address getDst() {
    return dst;
  }

  public void setDst(Address dst) {
    this.dst = dst;
  }

  private Address dst = null; // may be null; other address participating in
  // event

  public SimulatorEvent(Address src, Address dst) {
    this.src = src;
    this.dst = dst;
  }

  public SimulatorEvent(Address src) {
    this(src, null);
  }

  public void cancel() {
    cancelled = true;
  }

  public void handleError(MicaException e, Simulator simulator) {
    // default exception handler

    if (e instanceof AbortRound) {
      abortRound(simulator);
    } else if (e instanceof FatalErrorHalt) {
      fatalErrorHalt(simulator);
    }
  }

  public void abortRound(Simulator simulator) {
    // TODO
  }

  public void fatalErrorHalt(Simulator simulator) {
    SimRuntime srcRuntime = simulator.getRuntime(src);
    simulator.killRuntime(srcRuntime);
  }

  @Override
  public int compareTo(SimulatorEvent e) {
    return Long.valueOf(t).compareTo(Long.valueOf(e.t));
  }

  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Returns how long the event took, in simulation time units. "now" is current time when event
   * starts
   *
   * @return
   */
  public abstract void execute(Simulator simulator) throws MicaException;

  public void lockAcquired(Simulator simulator) throws MicaException {
    // override
  }

  public String toString() {
    SimulatorEvent e = this;
    if (getDst() != null) {
      return String
          .format("%s (%x) [%s -- %s]", e.getClass().getSimpleName(), e.hashCode(), e.getSrc(),
              e.getDst());
    } else {
      return String.format("%s (%x) [%s]", e.getClass().getSimpleName(), e.hashCode(), e.getSrc());

    }

  }
}