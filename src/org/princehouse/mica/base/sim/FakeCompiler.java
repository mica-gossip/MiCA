package org.princehouse.mica.base.sim;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaException;
import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.MicaRuntime;

public class FakeCompiler extends Compiler {

	@Override
	public CommunicationPatternAgent compile(Protocol pinstance) {
		return new FakeCommunicationPatternAgent();
	}

	/**
	 * When running experiments with all nodes in one JVM, use a side channel to
	 * pass state between sender and receiver. A null value is serialized.
	 * 
	 * note that f1, f2, and f3 for a gossip exchange must all be called in
	 * succession --- if any other exchanges f1/f2/f3 are interleaved within the
	 * same thread, corruption will result.
	 * 
	 * @author lonnie
	 * 
	 */
	public static class FakeCommunicationPatternAgent implements
			CommunicationPatternAgent {

		private ThreadLocal<MicaRuntime> initiator = new ThreadLocal<MicaRuntime>();

		@Override
		public Serializable f1(MicaRuntime initiatorRuntime) throws MicaException {
			assert (initiator.get() == null);
			initiator.set(initiatorRuntime);
			return null;
		}

		@Override
		public Serializable f2(MicaRuntime receiverRuntime, Serializable m1)
				throws FatalErrorHalt, AbortRound {
			MicaRuntime i = initiator.get();
			MiCA.getRuntimeInterface().getRuntimeContextManager()
					.setNativeRuntime(receiverRuntime);
			MiCA.getRuntimeInterface()
					.getRuntimeContextManager()
					.setForeignRuntimeState(i.getProtocolInstance(),
							i.getRuntimeState());
			try {
				i.getProtocolInstance().update(
						receiverRuntime.getProtocolInstance());
			} catch (Throwable t) {
				receiverRuntime.handleError(RuntimeErrorCondition.UPDATE_EXCEPTION, t);
			} finally {
				initiator.remove();
				MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
			}
			return null;
		}

		@Override
		public void f3(MicaRuntime initiatorRuntime, Serializable m2)
				throws FatalErrorHalt, AbortRound {
			// do nothing
		}

		
	}
}
