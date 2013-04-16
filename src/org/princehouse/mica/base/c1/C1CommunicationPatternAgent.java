package org.princehouse.mica.base.c1;

import java.io.Serializable;

import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.util.Serialization;

import com.esotericsoftware.kryo.Kryo;

public class C1CommunicationPatternAgent implements
		CommunicationPatternAgent {

	private Kryo kryo = null;
	
	public C1CommunicationPatternAgent(Kryo kryo) {
		this.kryo = kryo;
	}
	
	
	@SuppressWarnings("serial")
	protected static class C1Message implements Serializable {
		public C1Message() {} 
	
		public C1Message(Protocol p, RuntimeState runtimeState) {
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
	public Serializable f1(MicaRuntime initiatorRuntime) {
		try {
			return new C1Message(initiatorRuntime.getProtocolInstance(),
					initiatorRuntime.getRuntimeState());
		} finally {
			MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
		}
	}

	@Override
	public Serializable f2(MicaRuntime receiverRuntime, Serializable o)
			throws FatalErrorHalt, AbortRound {
		C1Message m1 = (C1Message) o;
		MiCA.getRuntimeInterface().getRuntimeContextManager()
				.setNativeRuntime(receiverRuntime);
		try {
			MiCA.getRuntimeInterface().getRuntimeContextManager()
					.setForeignRuntimeState(m1.p, m1.runtimeState);
			try {
				m1.p.update(receiverRuntime.getProtocolInstance());
			} catch (Throwable t) {
				receiverRuntime.handleError(
						RuntimeErrorCondition.UPDATE_EXCEPTION, t);
			}
			return m1;
		} finally {
			MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
		}
	}

	@Override
	public void f3(MicaRuntime initiatorRuntime, Serializable o) {
		C1Message m2 = (C1Message) o;
		initiatorRuntime.setProtocolInstance(m2.p);
		initiatorRuntime.setRuntimeState(m2.runtimeState);
	}

	@Override
	public byte[] serialize(Serializable obj) {
		return Serialization.serializeDefault(obj);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Serializable> T deserialize(byte[] data) {
		return (T) Serialization.deserializeDefault(data);
	}

}
