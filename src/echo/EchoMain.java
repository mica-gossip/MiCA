package echo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.base.simple.SimpleRuntimeInterface;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.Array;
import org.princehouse.mica.util.jconverters.ArgsConverterFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * The Launcher class starts a single instance of a MiCA protocol on the local
 * machine.
 * 
 * Command line arguments can easily be passed to the protocol using JCommander
 * for parsing
 * 
 * See the Launcher tutorial on the MiCA wiki.
 * 
 * @author lonnie
 * 
 */
public class EchoMain {

    private String protocolClassString = null;
    private Class<?> protocolClass = null;
    private Protocol protocolInstance = null;

    private String thisaddr = "127.0.0.1:12345";// "10.32.233.129:12345";
    @Parameter(names = "-address", description = "Address")
    private Address address = new TCPAddress(thisaddr);

    @Parameter(names = "-round", description = "Gossip round length (milliseconds)")
    private int intervalMS = 1000;

    @Parameter(names = "-seed", description = "Random seed (long int)")
    private long randomSeed = 0L;

    // not working... jcommander bug? implement as exceptional case
    @Parameter(names = "-usage", description = "Print usage", arity = 0)
    public boolean printUsage = false;

    @Parameter(names = "-timeout", description = "Lock waiting timeout (ms)")
    public int timeout = 30000;

    private String peeraddr = "127.0.0.1:12346"; // "10.32.55.4:12345";
    @Parameter(names = "-peeraddress", description = "Peer Address")
    private Address peeraddress = new TCPAddress(peeraddr);

    /**
     * Usage: Launcher <protocol class name> [launcher arguments] [protocol
     * arguments]
     * 
     * Launch a single instance of a MiCA protocol. The protocol can be passed
     * command line parameters by annotating it with JCommander \@Parameter
     * tags; for an example, see
     * 
     * Protocols launched this way must have a default constructor. If a method
     * named "initialize", taking no parameters, exists, it will be called on
     * the protocol instance after command line parameters are parsed.
     * 
     * @param args
     *            Command line arguments
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        new EchoMain().runMain(args);
    }

    private void fail(String fmt, Object... args) {
        System.err.println(String.format(fmt, args));
        System.exit(1);
    }

    private void fail(Throwable cause, String fmt, Object... args) {
        System.err.println(String.format(fmt, args));
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
        System.exit(1);
    }

    private void failPrintUsage() {
        System.err.println("Usage: Launcher <protocol class> [launcher args] [protocol args]");
        if (jc != null) {
            jc.usage();
        } else {
            jc = new JCommander(this);
            jc.addConverterFactory(new ArgsConverterFactory());
            jc.usage();
        }
        System.exit(1);
    }

    private JCommander jc = null;
    private MicaOptions options;
    private SimpleRuntimeInterface runtimeInterface;

    private void setOptions() {
        MicaOptions moptions = new MicaOptions();
        this.options = moptions;
        this.options.logdir = "C:/temp/MICALOG";
        MiCA.setOptions(this.options);
        // validate options and do option processing...
        String runtimeName = "simple";
        runtimeInterface = new SimpleRuntimeInterface();
        MiCA.setCompiler(runtimeInterface.getDefaultCompiler());
        runtimeInterface.reset();
        MiCA.setRuntimeInterface(runtimeInterface);
    }

    private void runMain(String[] args) throws InterruptedException {

        /*
         * if(args.length < 1) { failPrintUsage(); }
         */

        setOptions();

        protocolClassString = "echo.Echo";
        try {
            protocolClass = Class.forName(protocolClassString);
        } catch (ClassNotFoundException e) {
            fail("Class not found: %s", protocolClassString);
        }

        LinkedList<Address> peeraddrs = new LinkedList<Address>();
        peeraddrs.push(peeraddress);
        Echo echoHandler = new Echo(new StaticOverlay(peeraddrs));
        protocolInstance = (Protocol) (echoHandler);

        jc = new JCommander(new Object[] { this, protocolInstance });
        jc.addConverterFactory(new ArgsConverterFactory());

        /*
         * String[] subargs = Array.subArray(args, 1, args.length-1);
         * 
         * for(String s : subargs) { if(s.equals("-usage")) // workaround for
         * jcommander brokenness printUsage = true; }
         * 
         * if(printUsage) { failPrintUsage(); }
         * 
         * 
         * jc.parse(subargs);
         */
        // Attempt to run the initialize method
        runInitialize(protocolClass, protocolInstance);
        SimpleRuntime srt = new SimpleRuntime(address);
        MiCA.getRuntimeInterface().getRuntimeContextManager().setNativeRuntime(srt);
        SimpleRuntime.launch(srt, protocolInstance, false, intervalMS, randomSeed, timeout);
        Thread.currentThread().sleep(1000);

        Integer count = 0;
        while (true) {
            System.out.println("Sending " + count);
            // srt.getProtocolInstanceLock().lock();
            echoHandler.sendMessage("Hello World " + count + " from " + thisaddr);
            // srt.getProtocolInstanceLock().unlock();
            Thread.currentThread().sleep(3000);
            count++;
        }
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