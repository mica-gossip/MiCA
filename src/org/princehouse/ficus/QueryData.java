package org.princehouse.ficus;

import java.io.Serializable;

public abstract class QueryData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract boolean isComplete();
}
