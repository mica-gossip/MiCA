package org.princehouse.mica.base.simple;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.model.CompilerException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.simple.Selector.SelectorAnnotationDecider;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.FunctionalReflection;
import org.princehouse.mica.util.MarkingObjectInputStream;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;

import fj.F;
import fj.P2;

/**
 * RuntimeAgent for the simple runtime.
 * 
 * @author lonnie
 * 
 * @param <P>
 *            Top-level Protocol class
 */
class SimpleRuntimeAgent<P extends Protocol> extends RuntimeAgent<P> {

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
	public SimpleRuntimeAgent(Class<P> pclass) throws CompilerException {
		this.pclass = pclass;
		process();
	}

	@Override
	public Address select(Runtime<?> rt, P pinstance, double randomValue) {
		// Sanity check to prevent self-gossip added by Josh Endries
		Distribution<Address> dist = getSelectDistribution(rt, pinstance);
		if (dist == null || dist.size() == 0) {
			/*
			 * Either the distribution doesn't exist or it's empty.
			 */
			return null;
		} else {
			Address a = dist.sample(rt.getRandom().nextLong());
			if (rt.getAddress().equals(a)) {
				/*
				 * Settle down now, we don't want to start talking to
				 * ourselves...
				 */
				return null;
			} else {
				/*
				 * We were lucky enough to get an address, toss it back!
				 */
				return a;
			}
		}
	}

	@Override
	public void gossip(Runtime<P> rt, P pinstance, Connection connection) {
		// 1. serialize local state, send over connection
		// 2. receive updated state
		// prerequisite of this agent: protocols implement serializable
		RequestMessage<P> msg = new RequestMessage<P>(pinstance,
				rt.getRuntimeState());

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			oos.writeObject(msg);
		} catch (IOException e) {
			rt.punt(e);
		}

		ObjectInputStream ois;
		try {
			try {
				ois = new ObjectInputStream(connection.getInputStream());
			} catch (SocketException e) {
				((BaseProtocol) pinstance)
				.logCsv("gossip-init-connection-failure");
				return;
			}
			try {
				@SuppressWarnings("unchecked")
				ResponseMessage<P> rpm = (ResponseMessage<P>) ois.readObject();

				rt.setProtocolInstance(rpm.protocolInstance);

				if (Runtime.LOGGING_CSV)
					((BaseProtocol) rpm.protocolInstance).logstate();

				rt.logJson("state", rpm.protocolInstance.getLogState());

				// Update runtime state
				rt.getRuntimeState().update(rpm.runtimeState);

			} catch (ClassNotFoundException e) {
				rt.punt(e);
			}

		} catch (IOException e) {
			rt.tolerate(e);
		}

		try {
			connection.close();
		} catch (IOException e) {
			rt.tolerate(e);
		}
	}

	/**
	 * Validate the Protocol class and locate its select, update, rate methods.
	 * 
	 * @throws CompilerException
	 */
	private void process() throws CompilerException {
		// TODO needs sanity check that protocol implements serializable

		try {
			locateSelectMethod(pclass);
		} catch (TooManyException e) {
			throw new CompilerException("Failure to identify protocol select",
					e);
		} catch (NotFoundException e) {
			throw new CompilerException(String.format(
					"Failure to identify protocol select for %s",
					pclass.getName()), e);
		} catch (InvalidSelectElement e) {
			throw new CompilerException(e.toString(), e);
		} catch (ConflictingSelectAnnotationsException e) {
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
		Runtime.debug
		.printf("SimpleRuntimeAgent processing for %s:\n   select = %s\n   update = %s\n   freq = %s\n",
				pclass.getName(), selector, updateMethod,
				frequencyMethod);
				*/
		
	}

	private void locateUpdateMethod() throws TooManyException,
	NotFoundException {
		// TODO sanity check that update has the right signature
		try {
			updateMethod = Functional.findExactlyOne(
					(Iterable<Method>) FunctionalReflection.getMethods(pclass),
					FunctionalReflection
					.<Method> hasAnnotation(GossipUpdate.class));
		} catch (TooManyException e) {
			// If multiple options are found, see if one overrides the others by
			// sorting by declaring class subclass relation
			List<Method> options = Functional.mapcast(e.getOptions());
			HashMap<Class<?>, List<Method>> groups = Functional.groupBy(
					options, FunctionalReflection.getOriginatingClassMethod);
			List<P2<Class<?>, List<Method>>> items = Functional.items(groups);
			Collections.sort(items, Functional
					.pcomparator(FunctionalReflection.subclassComparator));
			List<Method> methods = items.get(0)._2();
			if (methods.size() > 1)
				throw new TooManyException(Functional.mapcast(methods));
			else {
				updateMethod = methods.get(0);
			}

		}
	}

	private void locateFrequencyMethod() throws TooManyException,
	NotFoundException {
		// TODO sanity check that freq has the right signature
		try {
			frequencyMethod = Functional.findExactlyOne(
					(Iterable<Method>) FunctionalReflection.getMethods(pclass),
					FunctionalReflection
					.<Method> hasAnnotation(GossipRate.class));
		} catch (TooManyException e) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Method> options = (List) e.getOptions();// Functional.mapcast(
			// e.getOptions().get(0));
			HashMap<Class<?>, List<Method>> groups = Functional.groupBy(
					options, FunctionalReflection.getOriginatingClassMethod);
			List<P2<Class<?>, List<Method>>> items = Functional.items(groups);
			Collections.sort(items, Functional
					.pcomparator(FunctionalReflection.subclassComparator));
			List<Method> methods = items.get(0)._2();
			if (methods.size() > 1)
				throw new TooManyException(Functional.mapcast(methods));
			else {
				frequencyMethod = methods.get(0);
			}
		}
	}

	private void locateSelectMethod(Class<?> klass) throws NotFoundException,
	TooManyException, InvalidSelectElement, ConflictingSelectAnnotationsException {
		/*
		 * Search through fields and functions looking for syntactic sugar
		 * Select annotations
		 */

		// first class function that tells us if an AnnotatedElement has any of the registered select annotations
		F<AnnotatedElement, Boolean> hasSelectAnnotation1 = Functional
				.<F<AnnotatedElement, Boolean>> foldl(Functional
						.<AnnotatedElement> or1(), Functional.map(
								Selector.registeredDeciders,
								SelectorAnnotationDecider.getAccept));

		AnnotatedElement selectElement = null;
		try {
			// search for annotated element in the given class (and its ancestor
			// classes)
			try {
				selectElement = Functional.findExactlyOne(
						(Iterable<AnnotatedElement>) FunctionalReflection
						.getAnnotatedElements(klass), hasSelectAnnotation1);
			} catch (NotFoundException nf) {
				Class<?> base = klass.getSuperclass();
				if (base == null)
					throw nf;
				else
					locateSelectMethod(base);
			}
		} catch (TooManyException e) {
			// we found more than one annotated element!
			// sort them by order of declaring class w.r.t.
			// inheritance hierarchy

			// get all of the annotated elements
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<AnnotatedElement> options = (List) e.getOptions();

			// group annotated elements by declaring class
			HashMap<Class<?>, List<AnnotatedElement>> groups = Functional
					.groupBy(options, FunctionalReflection
							.<AnnotatedElement> getOriginatingClass());

			// rearrange grouping map as a list of key,value pairs
			List<P2<Class<?>, List<AnnotatedElement>>> items = Functional
					.items(groups);

			// sort by subclass relation
			Collections.sort(items, Functional
					.pcomparator(FunctionalReflection.subclassComparator));

			// get elements of the least class (i.e., the farthest descendant
			// subtype)
			List<AnnotatedElement> elements = items.get(0)._2();

			if (elements.size() > 1)
				// More than one declared select element in this class!
				throw new TooManyException(Functional.mapcast(elements));
			else {
				// Great, we decided on exactly one!
				selectElement = elements.get(0);
			}
		}

		final AnnotatedElement temp = selectElement;

		// Which Selectors accept this element?
		List<SelectorAnnotationDecider> acceptingDeciders = Functional.list(Functional.filter(Selector.registeredDeciders, new F<SelectorAnnotationDecider,Boolean>() {
			@Override
			public Boolean f(SelectorAnnotationDecider d) {
				return d.accept(temp);
			}
		}));

		switch(acceptingDeciders.size()) {
		case 0:
			throw new RuntimeException("Something has gone horribly wrong. Even though a decider chose this selectElement earlier, no deciders now accept it!");
		case 1:
			selector = acceptingDeciders.get(0).getSelector(selectElement);
			break;
		default:
			throw new ConflictingSelectAnnotationsException(selectElement);
		}

	}

	/**
	 * Callback executed when a gossip request arrives. Deserializes a
	 * RequestMessage from the incoming connection and sends back a
	 * ResponseMessage.
	 * 
	 * @param runtime
	 *            Current Runtime
	 * @param pinstance
	 *            Protocol instance
	 * @param connection
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void acceptConnection(Runtime<?> runtime, P pinstance,
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
			P p1 = rqm.protocolInstance;

			// foreign state is used by the visiting node to access remote
			// runtime state data
			SimpleRuntime<?> srt = (SimpleRuntime<?>) runtime;

			srt.setForeignState(ois.getForeignObjectSet(), rqm.runtimeState);
			try {
				runGossipUpdate(runtime, p1, pinstance);
				((BaseProtocol) pinstance).logstate();

			} catch (RuntimeException e) {
				runtime.handleUpdateException(e);
			}
			srt.clearForeignState();

			ObjectOutputStream oos = new ObjectOutputStream(
					connection.getOutputStream());
			ResponseMessage<P> rpm = new ResponseMessage<P>(p1,
					rqm.runtimeState);
			oos.writeObject(rpm);
			oos.close();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// rt.setProtocolInstance(pinstance);
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

	@Override
	public Distribution<Address> getSelectDistribution(Runtime<?> rt,
			P pinstance) {
		return selector.select(rt, pinstance);
	}

	public void executeUpdate(Runtime<?> rt, P p1, P p2) {
		runGossipUpdate(rt, p1, p2);
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

	@Override
	public void handleNullSelect(Runtime<?> runtime, P pinstance) {
	}

	@Override
	public void handleConnectException(Runtime<?> runtime, P pinstance,
			Address partner, ConnectException ce) {
		((BaseProtocol) pinstance).logCsv("connect-exception,%s", partner);

		// TODO add hook for user defined connect error handlers

	}

}
