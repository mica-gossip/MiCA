package org.princehouse.mica.base.runtime;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;


public class SelectMethodSelector<Q extends Protocol> extends Selector<Q> {

	private Method method;
	
	public SelectMethodSelector(Method method) {
		this.method = method;
		validate(method);
	}
	
	private void validate(Method Method) {
		// FIXME: sanity check should go here
	}
	
	
	@Override
	public Distribution<Address> select(Runtime<?> rt, Q pinstance) {
		try {
			return (Distribution<Address>) method.invoke(pinstance);
		} catch (IllegalArgumentException e) {
			rt.tolerate(e);
		} catch (IllegalAccessException e) {
			rt.tolerate(e);
		} catch (InvocationTargetException e) {
			rt.handleSelectException(e);
		}
		return null;
	}
	
	public String toString() {
		return String.format("<%s Method %s>",getClass().getName(), method.getName());
	}
}