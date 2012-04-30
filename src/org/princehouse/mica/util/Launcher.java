package org.princehouse.mica.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.util.jconverters.ArgsConverterFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * The Launcher class starts a single instance of a MiCA protocol on the local machine. 
 * 
 * Command line arguments can easily be passed to the protocol using JCommander for parsing
 * 
 * See the Launcher tutorial on the MiCA wiki.
 * 
 * @author lonnie
 *
 */
public class Launcher {

	private String protocolClassString = null;
	private Class<?> protocolClass = null;
	private Protocol protocolInstance = null;
	
	@Parameter(names = "-address", description = "Address")
	private Address address = new TCPAddress("localhost:8000"); 
	
	@Parameter(names = "-round", description = "Gossip round length (milliseconds)")
	private int intervalMS = SimpleRuntime.DEFAULT_INTERVAL;
	
	@Parameter(names = "-seed", description = "Random seed (long int)")
	private long randomSeed = SimpleRuntime.DEFAULT_RANDOM_SEED;
	
	// not working... jcommander bug?  implement as exceptional case
	@Parameter(names = "-usage", description = "Print usage", arity=0)
	public boolean printUsage = false;
	
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
		if(cause != null) {
			cause.printStackTrace(System.err);
		}
		System.exit(1);
	}
	
	private void failPrintUsage() {
		System.err.println("Usage: Launcher <protocol class> [launcher args] [protocol args]");
		if(jc != null) {
			jc.usage();
		} else {
			jc = new JCommander(this);
			jc.addConverterFactory(new ArgsConverterFactory());
			jc.usage();
		}
		System.exit(1);
	}
	
	private JCommander jc = null;
	
	private void runMain(String[] args) {
	
		if(args.length < 1) {
			failPrintUsage();
		}
		
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
			fail(e,"Exception while instantiating %s:",protocolClassString);
		} catch (IllegalAccessException e) {
			fail("Illegal accesss exception: Is the default constructor for %s public?",protocolClassString);
		}
		
		jc = new JCommander(new Object[]{this,protocolInstance}); 
		jc.addConverterFactory(new ArgsConverterFactory());
		
		String[] subargs = Array.subArray(args, 1, args.length-1);

		for(String s : subargs) {
			if(s.equals("-usage")) // workaround for jcommander brokenness
				printUsage = true;
		}
		
		if(printUsage) {
			failPrintUsage();
		}
		
		
		jc.parse(subargs);
		// Attempt to run the initialize method
		runInitialize(protocolClass, protocolInstance);		
		SimpleRuntime.launch(protocolInstance, address, false, intervalMS, randomSeed);
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
