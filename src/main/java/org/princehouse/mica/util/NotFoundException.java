package org.princehouse.mica.util;

public class NotFoundException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public String toString() {
        String prev = super.toString();
        return "Error: Cannot locate View object. If you have @View or @UniformView annotated fields, please make them public! " + prev;
    }
}
