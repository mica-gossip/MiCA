package org.princehouse.mica.util;

import java.util.List;

public class TooManyException extends Exception {

	private List<Object> options;
	
	public TooManyException(List<Object> temp) {
		this.options= temp;
	}

	public TooManyException(String msg) {
		super(msg);
	}

	public List<Object> getOptions() {
		return options;
	}
	
	private static final long serialVersionUID = 1L;
}
