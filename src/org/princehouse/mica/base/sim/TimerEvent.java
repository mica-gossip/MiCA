package org.princehouse.mica.base.sim;

import java.util.TimerTask;

import org.princehouse.mica.base.exceptions.MicaRuntimeException;
import org.princehouse.mica.base.net.model.Address;

public class TimerEvent extends SimulatorEvent {

	private TimerTask task = null;
	
	public TimerEvent(Address src, TimerTask task) {
		super(src);
		this.task = task;
	}
	
	@Override
	public void execute(Simulator simulator) throws MicaRuntimeException {
		task.run();
	}

}
