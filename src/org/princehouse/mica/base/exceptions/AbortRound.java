package org.princehouse.mica.base.exceptions;

import org.princehouse.mica.base.RuntimeErrorCondition;

public class AbortRound extends MicaException {

    public AbortRound() {
        super(null, null);
    }

    public AbortRound(RuntimeErrorCondition condition, Throwable exception) {
        super(condition, exception);
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

}
