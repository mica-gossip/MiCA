package org.princehouse.mica.base.simple;

import java.io.Serializable;

import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.simple.SimpleCommunicationPatternAgent.SimpleM;

public class SimpleCommunicationPatternAgent implements
		CommunicationPatternAgent<SimpleM, SimpleM> {

	@SuppressWarnings("serial")
	protected static class SimpleM implements Serializable {
		public SimpleM(Protocol p, RuntimeState runtimeState) {
			this.p = p;
			this.runtimeState = runtimeState;
		}
		private Protocol p;
		public Protocol getP() {
			return p;
		}
		public RuntimeState getRuntimeState() {
			return runtimeState;
		}
		private RuntimeState runtimeState;
	}


	@Override
	public SimpleM f1(Protocol a) {
		return new SimpleM(a, a.getRuntimeState());
	}

	@Override
	public SimpleM f2(Protocol b, SimpleM m1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void f3(Protocol a, SimpleM m2) {
		// TODO Auto-generated method stub
		
	}

}
