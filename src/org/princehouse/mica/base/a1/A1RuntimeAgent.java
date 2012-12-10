package org.princehouse.mica.base.a1;

import static org.princehouse.mica.base.RuntimeErrorCondition.GOSSIP_IO_ERROR;
import static org.princehouse.mica.base.RuntimeErrorCondition.MISC_INTERNAL_ERROR;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.List;

import org.princehouse.mica.base.annotations.AnnotationInspector;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.CompilerException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.Logging.SelectEvent;
import org.princehouse.mica.util.MarkingObjectInputStream;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;
import org.princehouse.mica.util.soot.SootUtils;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

/**
 * RuntimeAgent for the simple runtime.
 * 
 * @author lonnie
 * 
 * @param <P>
 *            Top-level Protocol class
 */
class A1RuntimeAgent<P extends Protocol> extends RuntimeAgent<P> {

	/**
	 * Utility class representing the message sent by the gossip initiator to
	 * the gossip receiver. RequestMessage instances are serialized and sent
	 * over the network.
	 * 
	 * @author lonnie
	 * 
	 * @param <P>
	 *            Top-level protocol class
	 */
	protected static class RequestMessage<P extends Protocol> implements
			Serializable {
		private static final long serialVersionUID = 1L;

		public RequestMessage(P protocolInstance, RuntimeState runtimeState) {
			this.protocolInstance = protocolInstance;
			this.runtimeState = runtimeState;
		}

		private P protocolInstance;

		public P getProtocolInstance() {
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
	 * @param <P>
	 *            Top-level Protocol class
	 */
	protected static class ResponseMessage<P extends Protocol> implements
			Serializable {
		private static final long serialVersionUID = 1L;
		private P protocolInstance;
		private RuntimeState runtimeState;

		public ResponseMessage(P protocolInstance, RuntimeState runtimeState) {
			this.protocolInstance = protocolInstance;
			this.runtimeState = runtimeState;
		}

		public P getProtocolInstance() {
			return protocolInstance;
		}

		public RuntimeState getRuntimeState() {
			return runtimeState;
		}
	}

	private Class<P> pclass;

	private Selector<P> selector = null;

	private Method updateMethod;

	private Method frequencyMethod;

	/**
	 * Initialize Runtime Agent, including searching for select, update, rate
	 * annotated elements.
	 * 
	 * @param pclass
	 *            Top-level Protocol class
	 * @throws CompilerException
	 */
	public A1RuntimeAgent(Class<P> pclass) throws CompilerException {
		this.pclass = pclass;
		process();
	}

	private void partitionUpdate() {
		// partitions updateMethod according to locations
		// this -> local
		// param0 -> remote
		// TODO implement partitioning!

		// TODO first task: just run soot...

		String sootArgs[] = {};

		Options.v().parse(sootArgs);
		SootClass c = SootUtils.forceResolveJavaClass(
				updateMethod.getDeclaringClass(), SootClass.BODIES);
		c.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		SootMethod method = c.getMethodByName(updateMethod.getName());
		Scene.v().setEntryPoints(Functional.list(method));
		PackManager.v().runPacks();

	}

	@Override
	public SelectEvent select(Runtime<?> rt, P pinstance)
			throws SelectException {
		// Sanity check to prevent self-gossip added by Josh Endries
		// (and since moved into SimpleRuntimeAgent --- select() can return
		// self's address, but the runtime
		// will not gossip in this case
		SelectEvent rvalue = new SelectEvent();
		rvalue.view = getView(rt, pinstance);
		if (rvalue.view != null && rvalue.view.size() > 0) {
			rvalue.selected = rvalue.view.sample(rt.getRandom().nextLong());
		}
		return rvalue;
	}

	@Override
	public void gossip(Runtime<P> rt, P pinstance, Connection connection)
			throws AbortRound, FatalErrorHalt {
		// 1. serialize local state, send over connection
		// 2. receive updated state
		// prerequisite of this agent: protocols implement serializable
		RequestMessage<P> msg = new RequestMessage<P>(pinstance,
				rt.getRuntimeState());

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			oos.writeObject(msg);
		} catch (SocketException se) {
			Object mg;
			if (A1Runtime.DEBUG_NETWORKING)
				mg = se;
			else
				mg = se.getMessage();
			rt.handleError(GOSSIP_IO_ERROR, mg);
		} catch (IOException e) {
			Object mg;
			if (A1Runtime.DEBUG_NETWORKING)
				mg = e;
			else
				mg = e.getMessage();
			rt.handleError(GOSSIP_IO_ERROR, mg);
		}

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(connection.getInputStream());
		} catch (SocketException e) {
			Object mg;
			if (A1Runtime.DEBUG_NETWORKING)
				mg = e;
			else
				mg = e.getMessage();
			rt.handleError(GOSSIP_IO_ERROR, mg);
		} catch (IOException e) {
			Object mg;
			if (A1Runtime.DEBUG_NETWORKING)
				mg = e;
			else
				mg = e.getMessage();
			rt.handleError(GOSSIP_IO_ERROR, mg);
		}

		try {
			@SuppressWarnings("unchecked")
			ResponseMessage<P> rpm = (ResponseMessage<P>) ois.readObject();

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
	}

	/**
	 * Validate the Protocol class and locate its select, update, rate methods.
	 * 
	 * @throws CompilerException
	 */
	private void process() throws CompilerException {
		// TODO needs sanity check that protocol implements serializable

		// FIXME real analysis goes here
		// note that calling sub-updates will go through Protocol.executeUpdate
		System.out.printf("A1: compile class %s\n", pclass.getName());

		try {
			locateSelectMethod(pclass);
		} catch (TooManyException e) {
			throw new CompilerException("Failure to identify protocol select",
					e);
		} catch (NotFoundException e) {
			throw new CompilerException(String.format(
					"Failure to identify protocol select for %s",
					pclass.getName()), e);
		} catch (SelectException e) {
			throw new CompilerException(e.toString(), e);
		}

		try {
			locateUpdateMethod();
		} catch (TooManyException e) {
			throw new CompilerException("Failure to identify protocol update",
					e);
		} catch (NotFoundException e) {
			throw new CompilerException("Failure to identify protocol update",
					e);
		}

		partitionUpdate();

		try {
			locateFrequencyMethod();
		} catch (TooManyException e) {
			throw new CompilerException(
					"Failure to identify protocol frequency", e);
		} catch (NotFoundException e) {
			throw new CompilerException(
					"Failure to identify protocol frequency", e);
		}

		/*
		 * Runtime.debug .printf(
		 * "SimpleRuntimeAgent processing for %s:\n   select = %s\n   update = %s\n   freq = %s\n"
		 * , pclass.getName(), selector, updateMethod, frequencyMethod);
		 */
	}

	private void locateUpdateMethod() throws TooManyException,
			NotFoundException {
		updateMethod = AnnotationInspector.locateUpdateMethod(pclass);
	}

	private void locateFrequencyMethod() throws TooManyException,
			NotFoundException {
		frequencyMethod = AnnotationInspector.locateFrequencyMethod(pclass);
	}

	private void locateSelectMethod(Class<?> klass) throws NotFoundException,
			TooManyException, SelectException {
		selector = AnnotationInspector.<P> locateSelectMethod(klass);
	}

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
	 * @param receiverState
	 *            Protocol instance
	 * @param connection
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void acceptConnection(Runtime<?> runtime, P receiverState,
			Connection connection) throws IOException {

		MarkingObjectInputStream ois = null; // can distinguish deserialized
		// objects from others

		try {
			ois = new MarkingObjectInputStream(connection.getInputStream());
		} catch (java.io.EOFException e) {
			runtime.tolerate(e);
			return;
		}
		try {
			RequestMessage<P> rqm = (RequestMessage<P>) ois.readObject();
			P initiatorState = rqm.protocolInstance;

			// foreign state is used by the visiting node to access remote
			// runtime state data
			A1Runtime<?> srt = (A1Runtime<?>) runtime;

			srt.setForeignState(ois.getForeignObjectSet(), rqm.runtimeState);
			try {
				runGossipUpdate(runtime, initiatorState, receiverState);
			} catch (RuntimeException e) {
				runtime.handleUpdateException(e);
			}

			runtime.logState("gossip-receiver");

			srt.clearForeignState();

			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			ResponseMessage<P> rpm = new ResponseMessage<P>(initiatorState,
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
	private void runGossipUpdate(Runtime<?> runtime, P pinit, P precv) {
		// imperative update of p1 and p2 states
		try {
			updateMethod.invoke(pinit, precv);
		} catch (IllegalArgumentException e) {
			runtime.fatal(e);
		} catch (IllegalAccessException e) {
			runtime.fatal(e);
		} catch (InvocationTargetException e) {
			Throwable tgt = e.getTargetException();
			if (tgt instanceof RuntimeException)
				throw (RuntimeException) tgt;
			else {
				throw new RuntimeException(e); // shouldn't happen --- update
				// doesn't declare any
				// exceptions; anything
				// thrown must be a
				// RuntimeException
			}
		}
	}

	public void executeUpdate(Runtime<?> rt, P p1, P p2) {
		runGossipUpdate(rt, p1, p2);
	}

	@Override
	public Distribution<Address> getView(Runtime<?> rt, P pinstance)
			throws SelectException {
		return selector.select(rt, pinstance);
	}

	@Override
	public double getRate(Runtime<?> rt, P pinstance) {
		try {
			return (Double) frequencyMethod.invoke(pinstance);
		} catch (IllegalArgumentException e) {
			return rt.fatal(e);
		} catch (IllegalAccessException e) {
			return rt.fatal(e);
		} catch (InvocationTargetException e) {
			Throwable tgt = e.getTargetException();
			if (tgt instanceof RuntimeException)
				throw (RuntimeException) tgt;
			else {
				throw new RuntimeException(e); // shouldn't happen --- update
				// doesn't declare any
				// exceptions, so anything
				// thrown should be a
				// runtimeexception
			}
		}

	}

}
