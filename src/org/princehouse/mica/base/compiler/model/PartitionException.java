package org.princehouse.mica.base.compiler.model;

@SuppressWarnings("serial")  // FIXME later
public class PartitionException extends Exception {
	private Exception e;
	public PartitionException(Exception e) {
		this.e = e;
	}
	public Exception getCause() {
		return e;
	}
}
