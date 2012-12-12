package org.princehouse.mica.base.sim;

import java.util.Date;

public class StopWatch {

	private long start = 0L;
	
	public StopWatch() {
		reset(); }
	
	public void reset() {
		start = getTime();
	}
	
	public long getTime() {
		return new Date().getTime();
		
	}
	
	public long elapsed() {
		return getTime() - start;
	}
	
}
