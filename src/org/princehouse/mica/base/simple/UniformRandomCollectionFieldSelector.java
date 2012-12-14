package org.princehouse.mica.base.simple;


import java.lang.reflect.Field;
import java.util.Collection;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;


class UniformRandomCollectionFieldSelector extends Selector {

	private Field field;
	
	public UniformRandomCollectionFieldSelector(Field field) {
		this.field = field;
		validate(field);
	}
	
	private void validate(Field field) {
		// FIXME: sanity check should go here
	}
	
	private Collection<Address> getCollection(Protocol pinstance) {
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
		try {
			return getCollectionFromValue(obj, pinstance.getRuntimeState());
		} catch (SelectException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Distribution<Address> select(Runtime rt, Protocol pinstance) {
		return Distribution.uniform(getCollection(pinstance));
	}
	
	public String toString() {
		return String.format("<%s field %s>",getClass().getName(), field.getName());
	}
}