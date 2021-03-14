package org.princehouse.mica.base.simple;

import java.lang.annotation.Annotation;

public class InvalidSelectValue extends SelectException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public InvalidSelectValue(Class<? extends Annotation> aclass, Object value) {
        this.value = value;
        this.aclass = aclass;
    }

    private Object value = null;

    private Class<? extends Annotation> aclass = null;

    public Class<? extends Annotation> getAclass() {
        return aclass;
    }

    public void setAclass(Class<? extends Annotation> aclass) {
        this.aclass = aclass;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toString() {
        return String.format("%s annotation cannot be paired with %s value %s", aclass.getName(),
                (value == null ? "(null)" : value.getClass().getName()), value);
    }
}
