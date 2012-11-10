package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Logging.SelectEvent;

public class SimRuntimeAgent<P extends Protocol> extends RuntimeAgent<P> {

	private P pinstance = null;
	
	public SimRuntimeAgent(P pinstance) {
		this.pinstance = pinstance;
	}

	@Override
	public SelectEvent select(Runtime<?> runtime, P pinstance,
			double randomValue) throws SelectException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Distribution<Address> getView(Runtime<?> runtime, P pinstance)
			throws SelectException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void gossip(Runtime<P> runtime, P pinstance, Connection connection)
			throws AbortRound, FatalErrorHalt {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getRate(Runtime<?> runtime, P pinstance) {
		// TODO Auto-generated method stub
		return 0;
	}

}
