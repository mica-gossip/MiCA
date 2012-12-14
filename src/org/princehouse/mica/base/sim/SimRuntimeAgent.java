package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Connection;

public class SimRuntimeAgent extends RuntimeAgent {

	public SimRuntimeAgent(Protocol pinstance) {
	}

	@Override
	public void gossip(Runtime runtime, Protocol pinstance,
			Connection connection) throws AbortRound, FatalErrorHalt {
		throw new UnsupportedOperationException();
	}
}
