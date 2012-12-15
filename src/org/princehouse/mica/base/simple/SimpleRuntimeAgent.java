package org.princehouse.mica.base.simple;

import static org.princehouse.mica.base.RuntimeErrorCondition.MISC_INTERNAL_ERROR;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.CompilerException;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeContextManager;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Connection;

/**
 * RuntimeAgent for the simple runtime.
 * 
 * @author lonnie
 * 
 * @param 
 *            Top-level Protocol class
 */
class SimpleRuntimeAgent extends RuntimeAgent {

	/**
	 * Utility class representing the message sent by the gossip initiator to
	 * the gossip receiver. RequestMessage instances are serialized and sent
	 * over the network.
	 * 
	 * @author lonnie
	 * 
	 * @param 
	 *            Top-level protocol class
	 */
	protected static class RequestMessage implements
			Serializable {
		private static final long serialVersionUID = 1L;

		public RequestMessage(Protocol protocolInstance, RuntimeState runtimeState) {
			this.protocolInstance = protocolInstance;
			this.runtimeState = runtimeState;
		}

		private Protocol protocolInstance;

		public Protocol getProtocolInstance() {
			return protocolInstance;
		}

		public RuntimeState getRuntimeState() {
			return runtimeState;
		}

		private RuntimeState runtimeState;
	}

	/**
	 * Utility class representing the response message sent by the gossip
	 * receiver to the initiator after receiving a RequestMessage.
	 * ResponseMessages are serialized and sent over the network.
	 * 
	 * @author lonnie
	 * 
	 * @param 
	 *            Top-level Protocol class
	 */
	protected static class ResponseMessage implements
			Serializable {
		private static final long serialVersionUID = 1L;
		private Protocol protocolInstance;
		private RuntimeState runtimeState;

		public ResponseMessage(Protocol protocolInstance, RuntimeState runtimeState) {
			this.protocolInstance = protocolInstance;
			this.runtimeState = runtimeState;
		}

		public Protocol getProtocolInstance() {
			return protocolInstance;
		}

		public RuntimeState getRuntimeState() {
			return runtimeState;
		}
	}

	/**
	 * Initialize Runtime Agent, including searching for select, update, rate
	 * annotated elements.
	 * 
	 * @param pclass
	 *            Top-level Protocol class
	 * @throws CompilerException
	 */
	public SimpleRuntimeAgent(Class<Protocol> pclass) throws CompilerException {
	}

	/*
	@Override
	public void gossip(Runtime rt, Protocol pinstance, Connection connection)
			throws AbortRound, FatalErrorHalt {
		// 1. serialize local state, send over connection
		// 2. receive updated state
		// prerequisite of this agent: protocols implement serializable
		RequestMessage msg = new RequestMessage(pinstance,
				rt.getRuntimeState());

		try {
			// send m1 to receiver
			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			oos.writeObject(msg);
		} catch (SocketException se) {
			rt.handleError(GOSSIP_IO_ERROR, se);
		} catch (IOException e) {
			rt.handleError(GOSSIP_IO_ERROR, e);
		}

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(connection.getInputStream());
		} catch (SocketException e) {
			rt.handleError(GOSSIP_IO_ERROR, e);
		} catch (IOException e) {
			rt.handleError(GOSSIP_IO_ERROR, e);
		}

		try {
			ResponseMessage rpm = (ResponseMessage) ois.readObject();

			rt.setProtocolInstance(rpm.protocolInstance);

			// Update runtime state
			rt.getRuntimeState().update(rpm.runtimeState);
		} catch (ClassNotFoundException e) {
			rt.handleError(MISC_INTERNAL_ERROR, e);
		} catch (IOException io) {
			rt.handleError(GOSSIP_IO_ERROR, io);
		}

		try {
			connection.close();
		} catch (IOException e) {
			rt.handleError(GOSSIP_IO_ERROR, e);
		}
	} */

	/**
	 * Callback executed when a gossip request arrives. Deserializes a
	 * RequestMessage from the incoming connection and sends back a
	 * ResponseMessage.
	 * 
	 * receiverState is modified in-place by the update method; for this reason,
	 * setProtocolInstance is never called
	 * 
	 * @param runtime
	 *            Current Runtime
	 * @param receiver
	 *            Protocol instance
	 * @param connection
	 * @throws IOException
	 * @throws AbortRound 
	 * @throws FatalErrorHalt 
	 */
	public void acceptConnection(Runtime runtime, Protocol receiver,
			Connection connection) throws IOException, FatalErrorHalt, AbortRound {

		ObjectInputStream ois = null; 

		try {
			ois = new ObjectInputStream(connection.getInputStream());
		} catch (java.io.EOFException e) {
			runtime.handleError(RuntimeErrorCondition.MISC_INTERNAL_ERROR, e);
		}
		try {
			RequestMessage rqm = (RequestMessage) ois.readObject();
			Protocol initiator = rqm.protocolInstance;

			// foreign state is used by the visiting node to access remote
			// runtime state data

			RuntimeContextManager context = MiCA.getRuntimeInterface().getRuntimeContextManager();
			context.setForeignRuntimeState(initiator, rqm.runtimeState);
			context.setNativeRuntime(runtime);
			try {
				try {
					initiator.update(receiver);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
				runtime.logState("gossip-receiver"); // sim-ok
			} catch (Throwable e) {
				runtime.handleError(RuntimeErrorCondition.UPDATE_EXCEPTION, e);
			}
			context.clear();
			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			ResponseMessage rpm = new ResponseMessage(initiator,
					rqm.runtimeState);
			oos.writeObject(rpm);
			oos.close();

		} catch (ClassNotFoundException e) {
			try {
				runtime.handleError(MISC_INTERNAL_ERROR, e);
			} catch (FatalErrorHalt e1) {
			} catch (AbortRound e1) {
			}
		}
	}

	/**
	 * Executes the gossip update function on two local instances.
	 * 
	 * @param runtime
	 *            Current runtime
	 * @param pinit
	 *            Initiating instance
	 * @param precv
	 *            Receiving instance
	 */
	private void runGossipUpdate(Runtime runtime, Protocol pinit, Protocol precv) {
		// imperative update of p1 and p2 states
		
	}
	
	public void executeUpdate(Runtime rt, Protocol p1, Protocol p2) {
		runGossipUpdate(rt, p1, p2);
	}

}
