package org.princehouse.mica.base.model;


public class MiCA {
	
	private static RuntimeInterface rti = null;
	private static MicaOptions options = null;
	private static Compiler compiler = null;
	
	public static void setRuntimeInterface(RuntimeInterface rti) {
		MiCA.rti = rti;
	}
	
	public static RuntimeInterface getRuntimeInterface() {
		return rti;
	}
	
	public static MicaOptions getOptions() {
		return options;
	}
	
	public static void setOptions(MicaOptions options) {
		MiCA.options = options;
	}
	
	public static Compiler getCompiler() {
		return compiler;
	}
	
	public static void setCompiler(Compiler compiler) {
		MiCA.compiler = compiler;
	}
}
