package org.princehouse.mica.base.c1;

import java.lang.reflect.Field;
import java.util.Set;

import org.princehouse.mica.util.Functional;

import com.esotericsoftware.kryo.Kryo;

public class ExclusiveFieldSerializer<T> extends ExtensibleFieldSerializer<T> {

	private Set<Class<?>> maskedTypes = Functional.set();
	private Set<Field> allowedFields = Functional.set();
	
	/**
	 * @param kryo
	 * @param type
	 * @param allowedFields A complete set of all fields that should be serialized in the object graph.
	 * @param maskedTypes A set of all types for which allowedFields will apply
	 */
	public ExclusiveFieldSerializer(Kryo kryo, Class<?> type, Set<Field> allowedFields, Set<Class<?>> maskedTypes) {
		super(kryo, type);
		this.allowedFields = allowedFields;
		this.maskedTypes = maskedTypes;
		rebuildCachedFields();
	}
	
	@Override
	protected boolean serializeField(Field field) {
		if(!maskedTypes.contains(field.getDeclaringClass())) 
			return super.serializeField(field);
		return allowedFields.contains(field);
	}
}

