package org.princehouse.mica.base.simple;


import java.lang.reflect.Field;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;


/**
 * Applies the @Select annotation to an object to produce a Distribution<Address>
 * 
 * Applicable object types:
 *    Distribution<Address>
 *    Protocol
 *    Overlay 
 *    Collection  (returns uniform random distribution)
 *    
 * @author lonnie
 *
 * @param <Q>
 */
public class SelectFieldSelector<Q extends Protocol> extends Selector<Q> {

	private Field field;

	public SelectFieldSelector(Field field) throws InvalidSelectElement {
		this.field = field;
		validate(field);
	}

	private void validate(Field field) throws InvalidSelectElement {
		// FIXME: sanity check should go here
	}

	@Override
	public Distribution<Address> select(Runtime<?> rt, Q pinstance) throws SelectException  {
		Object obj = null;
		try {
			obj = field.get(pinstance);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return asDistribution(obj, rt.getRuntimeState(pinstance));
	}

	

	public String toString() {
		return String.format("<%s field %s>",getClass().getName(), field.getName());
	}
}