package org.princehouse.mica.base.simple;


import java.lang.reflect.Field;
import java.util.Collection;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Distribution;


class UniformRandomCollectionFieldSelector<Q extends Protocol> extends Selector<Q> {

	private Field field;
	
	public UniformRandomCollectionFieldSelector(Field field) {
		this.field = field;
		validate(field);
	}
	
	private void validate(Field field) {
		// FIXME: sanity check should go here
	}
	
	private Collection<Address> getCollection(Q pinstance) {
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
		return getCollectionFromValue(obj);
	}

	@SuppressWarnings("unchecked")
	private Collection<Address> getCollectionFromValue(Object value) {
		if(value instanceof Collection) {
			// TODO add sanity check for addresses
			return (Collection<Address>) value;
		} else if(value instanceof Overlay) {
			return ((Overlay)value).getView();
		} else {
			throw new RuntimeException(String.format("Don't know how to extract view from %s instance", value.getClass().getName()));
		}
	}

	@Override
	public Distribution<Address> select(Runtime<?> rt, Q pinstance) {
		return Distribution.uniform(getCollection(pinstance));
		//return Randomness.choose(getCollection(pinstance));
	}
	
	public String toString() {
		return String.format("<%s field %s>",getClass().getName(), field.getName());
	}
}