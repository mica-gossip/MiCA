package org.princehouse.mica.base.simple;


import java.lang.reflect.Field;
import java.util.Collection;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Distribution;


class SelectFieldSelector<Q extends Protocol> extends Selector<Q> {

	private Field field;

	public SelectFieldSelector(Field field) {
		this.field = field;
		validate(field);
	}

	private void validate(Field field) {
		// FIXME: sanity check should go here
	}

	@Override
	public Distribution<Address> select(Runtime<?> rt, Q pinstance) {
		Object obj = null;
		try {
			obj = field.get(pinstance);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return getDistFromValue(obj);
	}

	@SuppressWarnings("unchecked")
	private Distribution<Address> getDistFromValue(Object obj) {
		if(obj instanceof Distribution) {
			return (Distribution<Address>) obj;
		} else if(obj instanceof Protocol) {
			return ((Protocol) obj).getSelectDistribution();
		} else {
			throw new RuntimeException(String.format("Don't know how to extract selector from %s instance", obj.getClass().getName()));
		}
	}

	public String toString() {
		return String.format("<%s field %s>",getClass().getName(), field.getName());
	}
}