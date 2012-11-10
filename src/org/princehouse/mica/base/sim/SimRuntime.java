package org.princehouse.mica.base.sim;

import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.MalformedViewException;
import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;

public class SimRuntime<P extends Protocol> extends Runtime<P> {

	private P instance = null;
	private RuntimeState runtimeState = new RuntimeState();
	private ReentrantLock lock = new ReentrantLock();
	
	@Override
	public ReentrantLock getProtocolInstanceLock() {
		return lock;
	}

	@Override
	public <T extends Protocol> RuntimeAgent<T> compile(T pinstance) {
		return new SimRuntimeAgent<T>(pinstance);
	}

	@Override
	public P getProtocolInstance() {
		return instance;
	}

	@Override
	public void setProtocolInstance(P pinstance) {
		instance = pinstance;
	}

	@Override
	public void stop() {
		getSimulator().stopRuntime(this);
	}

	@Override
	public Address getAddress() {
		return runtimeState.getAddress();
	}

	@Override
	public void setAddress(Address address) {
		runtimeState.setAddress(address);
		
	}

	@Override
	public RuntimeState getRuntimeState(Protocol p) {
		return getSimulator().getRuntimeState(p);
	}

	@Override
	public RuntimeState getRuntimeState() {
		return runtimeState;
	}

	@Override
	public Distribution<Address> getView(Protocol protocol) throws SelectException {
		Distribution<Address> view = compile(protocol).getView(this, protocol);
		if (view != null && view.isEmpty())
			return null;
		if (view != null && !view.isOne()) {
			throw new MalformedViewException(protocol, view);
		}
		return view;
	}

	@Override
	public void executeUpdate(Protocol p1, Protocol p2) {
		// TODO
		
	}

	@Override
	public double getRate(Protocol protocol) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void tolerateError() throws AbortRound {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void fatalErrorHalt(RuntimeErrorCondition condition)
			throws FatalErrorHalt {
		// TODO Auto-generated method stub
		
	}

	
	protected Simulator getSimulator() {
		return Simulator.v();
	}
}
