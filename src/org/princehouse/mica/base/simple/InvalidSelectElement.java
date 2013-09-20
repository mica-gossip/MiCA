package org.princehouse.mica.base.simple;

import java.lang.reflect.AnnotatedElement;

/**
 * Thrown when a @Select* annotation is attached to an invalid element
 * 
 * @author lonnie
 */
public class InvalidSelectElement extends SelectException {
    private static final long serialVersionUID = 1L;

    private Class<?> selectorClass;
    private AnnotatedElement element;

    public Class<?> getSelectorClass() {
        return selectorClass;
    }

    public void setSelectorClass(Class<?> selectorClass) {
        this.selectorClass = selectorClass;
    }

    public AnnotatedElement getElement() {
        return element;
    }

    public void setElement(AnnotatedElement element) {
        this.element = element;
    }

    public InvalidSelectElement(Class<?> selectorClass, AnnotatedElement element) {
        this.selectorClass = selectorClass;
        this.element = element;
    }

    public String toString() {
        return String.format("%s annotation cannot be used on element %s", selectorClass.getName(), element);
    }
}
