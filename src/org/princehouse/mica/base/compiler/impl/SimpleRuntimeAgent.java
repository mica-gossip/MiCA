package org.princehouse.mica.base.compiler.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.annotations.GossipFrequency;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.compiler.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.base.runtime.RuntimeState;
import org.princehouse.mica.base.runtime.SelectFieldSelector;
import org.princehouse.mica.base.runtime.SelectMethodSelector;
import org.princehouse.mica.base.runtime.Selector;
import org.princehouse.mica.base.runtime.UniformRandomCollectionFieldSelector;
import org.princehouse.mica.base.runtime.implementation.SimpleRuntime;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.FunctionalReflection;
import org.princehouse.mica.util.MarkingObjectInputStream;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;

import fj.F;
import fj.P2;

public class SimpleRuntimeAgent<P extends Protocol> extends RuntimeAgent<P> {

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

	public SimpleRuntimeAgent(Class<P> pclass) throws CPGCompileException {
		this.pclass = pclass;
		process();
	}

	@Override
	public Address select(Runtime<?> rt, P pinstance, double nextDouble) {
		Distribution<Address> dist = getSelectDistribution(rt, pinstance);
		if (dist == null)
			return null;
		else
			return dist.sample(rt.getRandom().nextLong());
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
				.log("gossip-init-connection-failure");
				return;
			}
			try {
				@SuppressWarnings("unchecked")
				ResponseMessage<P> rpm = (ResponseMessage<P>) ois.readObject();

				rt.setProtocolInstance(rpm.protocolInstance);

				((BaseProtocol) rpm.protocolInstance).logstate();

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

	// validate the supplied class
	private void process() throws CPGCompileException {
		// TODO needs sanity check that protocol implements serializable

		try {
			locateSelectMethod(pclass);
		} catch (TooManyException e) {
			throw new CPGCompileException(
					"Failure to identify protocol select", e);
		} catch (NotFoundException e) {
			throw new CPGCompileException(
					String.format("Failure to identify protocol select for %s", pclass.getName()), e);
		}

		try {
			locateUpdateMethod();
		} catch (TooManyException e) {
			throw new CPGCompileException(
					"Failure to identify protocol update", e);
		} catch (NotFoundException e) {
			throw new CPGCompileException(
					"Failure to identify protocol update", e);
		}

		try {
			locateFrequencyMethod();
		} catch (TooManyException e) {
			throw new CPGCompileException(
					"Failure to identify protocol frequency", e);
		} catch (NotFoundException e) {
			throw new CPGCompileException(
					"Failure to identify protocol frequency", e);
		}

		Runtime.debug
		.printf("SimpleRuntimeAgent processing for %s:\n   select = %s\n   update = %s\n   freq = %s\n",
				pclass.getName(), selector, updateMethod,
				frequencyMethod);
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
					.<Method> hasAnnotation(GossipFrequency.class));
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
	TooManyException {

		// first class functions for finding select and select uniform random annotations
		F<AnnotatedElement,Boolean> hasSelect = FunctionalReflection
				.<AnnotatedElement> hasAnnotation(Select.class);
		F<AnnotatedElement,Boolean> hasSelectUniformRandom = FunctionalReflection
				.<AnnotatedElement> hasAnnotation(SelectUniformRandom.class);

		AnnotatedElement selectElement = null;
		try {
			// search for annotated element in the given class
			try {
				selectElement = Functional
						.findExactlyOne(
								(Iterable<AnnotatedElement>) FunctionalReflection
								.getAnnotatedElements(klass),
								Functional.or(
										hasSelect,
										hasSelectUniformRandom));
			} catch(NotFoundException nf) {
				Class<?> base = klass.getSuperclass();
				if(base == null) 
					throw nf;
				else
					locateSelectMethod(base);
			}
		} catch (TooManyException e) {
			// we found too many.  sort them by order of declaring class w.r.t. inheritance hierarchy
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<AnnotatedElement> options = (List) e.getOptions();
			HashMap<Class<?>, List<AnnotatedElement>> groups = Functional.groupBy(
					options, FunctionalReflection.<AnnotatedElement>getOriginatingClass());
			List<P2<Class<?>, List<AnnotatedElement>>> items = Functional.items(groups);
			Collections.sort(items, Functional
					.pcomparator(FunctionalReflection.subclassComparator));
			List<AnnotatedElement> elements = items.get(0)._2();
			if (elements.size() > 1)
				throw new TooManyException(Functional.mapcast(elements));
			else {
				selectElement = elements.get(0);
			}
		}

		if(hasSelect.f(selectElement) && !hasSelectUniformRandom.f(selectElement)) {
			if(selectElement instanceof Method) {
				selector = new SelectMethodSelector<P>((Method)selectElement);
			} else if(selectElement instanceof Field) {
				selector = new SelectFieldSelector<P>((Field)selectElement);
			} else {
				throw new RuntimeException("Select annotation attached to invalid entity");
			}
		} else if(!hasSelect.f(selectElement) && hasSelectUniformRandom.f(selectElement)) {
			if(selectElement instanceof Field) {
				selector = new UniformRandomCollectionFieldSelector<P>((Field)selectElement);
			} else {
				throw new RuntimeException("Select annotation attached to invalid entity");
			}
		} else {
			throw new TooManyException(String.format("Element has multiple select annotations %s",selectElement)); // two annotations on the same element
		}

	}

	@SuppressWarnings("unchecked")
	public void acceptConnection(Runtime<?> rt, P pinstance, Address recipient,
			Connection connection) throws IOException {

		MarkingObjectInputStream ois = null; // can distinguish deserialized
		// objects from others

		try {
			ois = new MarkingObjectInputStream(connection.getInputStream());
		} catch (java.io.EOFException e) {
			rt.tolerate(e);
			return;
		}
		try {
			RequestMessage<P> rqm = (RequestMessage<P>) ois.readObject();
			P p1 = rqm.protocolInstance;

			// foreign state is used by the visiting node to access remote
			// runtime state data
			SimpleRuntime<?> srt = (SimpleRuntime<?>) rt;

			srt.setForeignState(ois.getForeignObjectSet(), rqm.runtimeState);
			try {
				runGossipUpdate(rt, p1, pinstance);
				((BaseProtocol) pinstance).logstate();

			} catch (RuntimeException e) {
				rt.handleUpdateException(e);
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

	private void runGossipUpdate(Runtime<?> rt, P p1, P p2) {
		// imperative update of p1 and p2 states
		try {
			updateMethod.invoke(p1, p2);
		} catch (IllegalArgumentException e) {
			rt.fatal(e);
		} catch (IllegalAccessException e) {
			rt.fatal(e);
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
	public Distribution<Address> getSelectDistribution(Runtime<?> rt,
			P pinstance) {
		return selector.select(rt, pinstance);
	}

	public void executeUpdate(Runtime<?> rt, P p1, P p2) {
		runGossipUpdate(rt, p1, p2);
	}

	@Override
	public double getFrequency(Runtime<?> rt, P pinstance) {
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
