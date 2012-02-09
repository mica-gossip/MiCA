package org.princehouse.mica.base.runtime;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;


public class SingleValueSelectFunctionSelector<Q extends Protocol> extends Selector<Q> {
	private Method selectMethod;
	
	public SingleValueSelectFunctionSelector(Method selectMethod) {
		validateSelectMethod(selectMethod);
		this.selectMethod = selectMethod;
	}
	
	private static void validateSelectMethod(Method selectMethod) {
		// TODO: throw exception if method invalid
	}
	
	@Override
	public Distribution<Address> select(Runtime<?> rt, Q pinstance) {
		try {
			Object robj = selectMethod.invoke(pinstance);
			Address addr = (Address) robj;
			return Distribution.create(addr);
		} catch (IllegalArgumentException e) {
			return rt.punt(e);
		} catch (IllegalAccessException e) {
			return rt.punt(e);
		} catch (InvocationTargetException e) {
			return rt.punt(e);
		}		
	}
	
	public String toString() {
		return String.format("<%s method %s>",getClass().getName(), selectMethod.getName());
	}
}