package org.princehouse.mica.base.sugar;

import java.lang.reflect.Method;
import java.util.Map;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.base.sugar.annotations.AnnotationInspector;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;

public class Sugar {

	public static Sugar v() {
		return singleton;
	}

	public Distribution<Address> executeSugarView(Protocol p) {
		try {
			Selector selector = getSelector(p);
			try {
				return selector.select(p);
			} catch (SelectException e) {
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public double executeSugarRate(Protocol p) {
		try {
			Method rateMethod = getRateMethod(p);
			try {
				return (Double) rateMethod.invoke(p);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// private ------------------------------------
	private Map<Class<?>, Selector> selectorCache = Functional.map();
	private Map<Class<?>, Method> rateCache = Functional.map();

	private Selector getSelector(Protocol p) throws NotFoundException,
			TooManyException, SelectException {
		Class<?> k = p.getClass();
		Selector s = selectorCache.get(k);
		if (s == null) {
			s = AnnotationInspector.locateSelectMethod(k);
			selectorCache.put(k, s);
		}
		return s;
	}

	private Method getRateMethod(Protocol p) throws TooManyException,
			NotFoundException {
		Class<?> k = p.getClass();
		Method m = rateCache.get(k);
		if (m == null) {
			m = AnnotationInspector.locateFrequencyMethod(p.getClass());
			rateCache.put(k, m);
		}
		return m;
	}

	private Sugar() {
	}

	private static Sugar singleton = new Sugar();
}
