package org.princehouse.mica.base.compiler.impl;


public class CPGCompileException extends Exception {

	public CPGCompileException(Exception e) {
		super(e);
	}

	public CPGCompileException(String msg, Exception e) {
		super(msg, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
