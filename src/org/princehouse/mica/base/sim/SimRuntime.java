package org.princehouse.mica.base.sim;

import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Address;

public class SimRuntime extends Runtime {

	private Protocol protocol = null;

	public SimRuntime(Address address) {
		setAddress(address);
	}

	@Override
	public String getLogFilename() {
		return String.format("%ssim_%s.log", MiCA.getOptions().logprefix,
				MiCA.getOptions().expname);
	}

	@Override
	public ReentrantLock getProtocolInstanceLock() {
		throw new RuntimeException();
	}

	@Override
	public RuntimeAgent compile(Protocol pinstance) {
		return new SimRuntimeAgent(pinstance);
	}

	@Override
	public void setProtocolInstance(Protocol pinstance) {
		protocol = pinstance;
	}

	@Override
	public void stop() {
		getSimulator().stopRuntime(this);
	}

	@Override
	public Address getAddress() {
		return getRuntimeState().getAddress();
	}

	@Override
	public void setAddress(Address address) {
		getRuntimeState().setAddress(address);

	}

	@Override
	public long getRuntimeClock() {
		return getSimulator().getClock();
	}

/*
	protected void tolerateError() throws AbortRound {
		throw new AbortRound(null, null);
	}

	protected void fatalErrorHalt(RuntimeErrorCondition condition)
			throws FatalErrorHalt {
		stop(); // passively signal that it's time to shut down
		try {
			getAddress().unbind();// try to unbind the listener
		} catch (NotBoundException e1) {
		}
		try {
			getProtocolInstanceLock().unlock();
		} catch (IllegalMonitorStateException e) {
		}
		throw new FatalErrorHalt();
	}
	*/

	protected Simulator getSimulator() {
		return Simulator.v();
	}

	@Override
	public void start() {
		initLog();
		MiCA.getRuntimeInterface().getRuntimeContextManager().setNativeRuntime(this);
		logState("initial");
		MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
	}

	@Override
	public Protocol getProtocolInstance() {
		return protocol;
	}
}
