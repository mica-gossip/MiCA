package org.princehouse.mica.base.model;

/**
 * General class for any compilation exception
 */
@SuppressWarnings("serial")
public class CompilerException extends Exception {
	private Exception e;
	private String msg = "Compiler exception";
	
	public CompilerException(Exception e) {
		this.e = e;
	}
	public CompilerException(String msg, Exception e) {
		this.msg = msg;
		this.e = e;
	}
	public Exception getCause() {
		return e;
	}
}
