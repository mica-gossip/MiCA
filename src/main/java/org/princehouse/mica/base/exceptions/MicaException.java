package org.princehouse.mica.base.exceptions;

import org.princehouse.mica.base.RuntimeErrorCondition;

public class MicaException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private RuntimeErrorCondition condition = null;

    public MicaException(RuntimeErrorCondition condition, Throwable exception) {
        super(exception);
        this.condition = condition;
    }

    public String toString() {
        return String.format("%s: %s", getClass().getSimpleName(), condition);
    }
}
