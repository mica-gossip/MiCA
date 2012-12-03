package org.princehouse.mica.util.harness;

import java.util.TimerTask;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;

public interface RuntimeInterface {

	
	/**
	 * Add a runtime but do not start it yet.  Call start function to start all runtimes.
	 * @param address 
	 * @param protocol
	 * @param randomSeed
	 * @param roundLength 
	 * @param startTime   Delay of initial sleep (in the runtime's units, probably ms)
	 */
	public <P extends Protocol> void addRuntime(Address address, P protocol, long randomSeed, int roundLength,int startTime, int lockTimeout);	
	
	/**
	 * Start all runtimes and block until they're finished
	 */
	public void run();

	public void scheduleTask(long delay, TimerTask task);

	public void stop();
	
}
