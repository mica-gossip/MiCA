package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.exceptions.MicaException;
import org.princehouse.mica.base.net.model.Address;

public class TimerEvent extends SimulatorEvent {

  private Runnable task = null;

  public TimerEvent(Address src, Runnable task) {
    super(src);
    this.task = task;
  }

  @Override
  public void execute(Simulator simulator) throws MicaException {
    task.run();
  }

}
