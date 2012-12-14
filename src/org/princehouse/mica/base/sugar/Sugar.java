package org.princehouse.mica.base.sugar;

import java.lang.reflect.Method;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.base.sugar.annotations.AnnotationInspector;
import org.princehouse.mica.util.Distribution;
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
	
	
	private Selector getSelector(Protocol p) throws NotFoundException, TooManyException, SelectException {
		return AnnotationInspector.locateSelectMethod(p.getClass());
	}
	
	private Method getRateMethod(Protocol p) throws TooManyException,
	NotFoundException {
		return AnnotationInspector.locateFrequencyMethod(p.getClass());
}

	
	private Sugar() {}
	
	private static Sugar singleton = new Sugar();
}
