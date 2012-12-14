package org.princehouse.mica.base.sim;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.princehouse.mica.base.annotations.AnnotationInspector;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Logging.SelectEvent;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;

public class SimRuntimeAgent extends RuntimeAgent {

	
	public SimRuntimeAgent(Protocol pinstance) {
	}

	@Override
	public SelectEvent select(Runtime runtime, Protocol pinstance) throws SelectException {
		SelectEvent rvalue = new SelectEvent();
		rvalue.view = getView(runtime, pinstance);
		if (rvalue.view != null && rvalue.view.size() > 0) {
			rvalue.selected = rvalue.view.sample(runtime.getRandom().nextLong());
		}
		return rvalue;
	}

	@Override
	public Distribution<Address> getView(Runtime runtime, Protocol pinstance)
			throws SelectException {
		try {
			Selector selector = AnnotationInspector.locateSelectMethod(pinstance.getClass());
			return selector.select(runtime, pinstance);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);			
		} catch (TooManyException e) {
			throw new RuntimeException(e);
		}
	}

	private Simulator getSimulator() {
		return Simulator.v();
	}
	
	@Override
	public void gossip(Runtime runtime, Protocol pinstance, Connection connection)
			throws AbortRound, FatalErrorHalt {
		try {
			Method rmethod = AnnotationInspector.locateUpdateMethod(pinstance.getClass());
			SimConnection sc = (SimConnection) connection;
			Protocol remote = getSimulator().getReceiver(sc);
			rmethod.invoke(pinstance, remote);
		} catch (TooManyException e) {
			throw new RuntimeException(e);			
		} catch (NotFoundException e) {
			throw new RuntimeException(e);			
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
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
		try {			
			Method umethod = null;
			try {
				umethod = AnnotationInspector.locateUpdateMethod(pinit.getClass());
			} catch (TooManyException e) {
				throw new RuntimeException(e);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
			umethod.invoke(pinit, precv);
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
	public void executeUpdate(Runtime rt, Protocol p1, Protocol p2) {
		runGossipUpdate(rt, p1, p2);
	}
	@Override
	public double getRate(Runtime runtime, Protocol pinstance) {
		try {
			Method rmethod = AnnotationInspector.locateFrequencyMethod(pinstance.getClass());
			Double d = (Double) rmethod.invoke(pinstance);
			return d;
		} catch (TooManyException e) {
			throw new RuntimeException(e);			
		} catch (NotFoundException e) {
			throw new RuntimeException(e);			
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
