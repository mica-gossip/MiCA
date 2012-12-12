package org.princehouse.mica.base.model;


public class MiCA {
	
	private static RuntimeInterface rti = null;
	
	public static void setRuntimeInterface(RuntimeInterface rti) {
		MiCA.rti = rti;
	}
	
	public static RuntimeInterface getRuntimeInterface() {
		return rti;
	}
}
