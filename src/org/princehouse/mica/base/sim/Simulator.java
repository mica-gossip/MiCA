package org.princehouse.mica.base.sim;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;

public class Simulator {

	
	protected void stopRuntime(SimRuntime<?> rt) {
		// TODO
	}
	
	
	// Simulator is a singleton...
	private static Simulator singleton = null;
	protected static Simulator v() {
		if(singleton == null) {
			singleton = new Simulator();
		}
		return singleton;
	}
	
	protected RuntimeState getRuntimeState(Protocol p) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
