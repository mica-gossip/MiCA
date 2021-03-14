package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.exceptions.MicaException;

public class RestartEvent extends SimulatorEvent {

  private SimRuntime rt = null;

  public RestartEvent(SimRuntime rt) {
    super(null);
    this.rt = rt;
  }

  @Override
  public void execute(Simulator simulator) throws MicaException {
    System.out.printf("restart node %s\n", rt.getAddress());
    simulator.bind(rt.getAddress(), rt, 0);
  }

}
