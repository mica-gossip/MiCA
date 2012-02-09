package org.princehouse.mica.base;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.runtime.RuntimeState;
import org.princehouse.mica.util.Distribution;

public interface Protocol {
	public RuntimeState getRuntimeState();
	
	public Distribution<Address> getSelectDistribution();
	
	public double getFrequency();

	public void executeUpdate(Protocol other);
	
	public static enum Direction {
		PUSH, PULL, PUSHPULL
	};
}
