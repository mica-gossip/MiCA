package org.princehouse.mica.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Launcher {

	private String protocolClassString = null;
	private Class<?> protocolClass = null;
	private Protocol protocolInstance = null;
	
	@Parameter(names = {"-address"}, description = "Address")
	public String addressString = "localhost:8000";
	
	/**
	 * Usage: Launcher <protocol class name> [launcher arguments] [protocol arguments]
	 * 
	 * Launch a single instance of a MiCA protocol.  The protocol can be passed command line parameters by annotating it with JCommander
	 * \@Parameter tags; for an example, see 
	 * 
	 * Protocols launched this way must have a default constructor.  If a method named "initialize", taking no parameters, exists,
	 * it will be called on the protocol instance after command line parameters are parsed.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		new Launcher().runMain(args);
	}
	
	private void fail(String fmt, Object... args) {
		System.err.println(String.format(fmt,args));
		System.exit(1);
	}
	
	private void fail(Throwable cause, String fmt, Object... args) {
		System.err.println(String.format(fmt,args));
		cause.printStackTrace(System.err);
		System.exit(1);
	}
	
	private void runMain(String[] args) {
		// TODO args length check
		protocolClassString = args[0];
		
		try {
			protocolClass = Class.forName(protocolClassString);
		} catch (ClassNotFoundException e) {
			fail("Class not found: %s", protocolClassString);
		}
		
		// instantiate class
		try {
			protocolInstance = (Protocol) protocolClass.newInstance();
		} catch (InstantiationException e) {
			fail(e.getCause(),"Exception while instantiating %s:",protocolClassString);
		} catch (IllegalAccessException e) {
			fail("Illegal accesss exception: Is the default constructor for %s public?",protocolClassString);
		}
		
		new JCommander(new Object[]{this,protocolInstance}, Array.subArray(args, 1, args.length-1));

		
		Address address = null;
		try {
			address = TCPAddress.valueOf(addressString);
		} catch (UnknownHostException e) {
			fail("Cannot interpret address \"%s\".  Addresses should be of the form \"host:port\"", addressString);
		}
		
		// Attempt to run the initialize method
		runInitialize(protocolClass, protocolInstance);
		
		SimpleRuntime.launch(protocolInstance, address, false);
	}
	
	private void runInitialize(Class<?> protocolClass, Protocol classInstance) {
		try {
			Method initialize = protocolClass.getMethod("initialize");
			try {
				initialize.invoke(classInstance);
			} catch (IllegalArgumentException e) {
				return; // can't happen
			} catch (IllegalAccessException e) {
				fail("IllegalAccessException: Is protocol initialize method public??");
			} catch (InvocationTargetException e) {
				fail(e.getCause(), "Exception while invoking %s.initialize()", protocolClass.getName());
			}
		} catch (SecurityException e) {
			fail("Protocol initialize() method is not public");
		} catch (NoSuchMethodException e) {
			return;
		}
	}

}
