package org.princehouse.mica.util.harness;

import java.io.File;
import java.io.FilenameFilter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.a1.A1RuntimeInterface;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeInterface;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.sim.Simulator;
import org.princehouse.mica.base.simple.SimpleRuntimeInterface;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Array;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.SinglyLinkedRingGraph;

import com.beust.jcommander.JCommander;

import fj.F;
import fj.F3;
import fj.P;
import fj.P2;

/**
 * TestHarness is used for running local experiments with many nodes on a
 * randomly-generated graph See examples.DemoCompositeProtocol for an example
 * 
 * Command line options for TestHarness are in the TestHarnessOptions inner
 * class
 * 
 * @author lonnie
 * 
 * @param 
 */
public class TestHarness {

	private RuntimeInterface runtimeInterface = null;
	
	public static final String LOG_NAMES = Array.join(", ", LogFlag.values());
	
	private List<P2<Long, TimerTask>> timers = Functional.list();

	public void addTimer(long time, TimerTask task) {
		timers.add(P.p(time, task));
	}

	private int getRoundMS() {
		return getOptions().roundLength;
	}

	public void addTimerRounds(double rounds, TimerTask task) {
		addTimer((long) (rounds * getRoundMS()), task);
	}

	private static int BASE_ADDRESS = 8000;

	public static F<Integer, Address> defaultAddressFunc = new F<Integer, Address>() {
		public Address f(Integer i) {
			try {
				return TCPAddress.valueOf(String.format("localhost:%d",
						BASE_ADDRESS + i));
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private Random random = null;
	public Random getRandom() {
		if(random == null) {
			random = new Random(getOptions().seed);
		}
		return random;
	}
	
	public TimerTask taskStop() {
		final TestHarness harness = this;
		return new TimerTask() {
			@Override
			public void run() {
				harness.stop();
				Runtime.debug.println("End timer!");
			}
		};
	}

	public void launchProtocol(ProtocolInstanceFactory factory,
			TestHarnessGraph g) {
		
		//List<Runtime> runtimes = Functional.list();
		//running = true;

		launchTimers(); // TODO lift to runtime interface

		
		int i = 0;
		
		for (Address addr : g.getAddresses()) {
			Overlay neighbors = g.getOverlay(addr);
			Protocol pinstance = factory.createProtocolInstance(i++, addr, neighbors);
			MicaOptions options = getOptions();
			int stagger = rng.nextInt(options.stagger);
			int lockTimeout = options.timeout;
			long seed = getRandom().nextLong();
			runtimeInterface.addRuntime(addr, pinstance, seed, options.roundLength, stagger, lockTimeout);		
		} 
	}


	private void launchTimers() {
		for (P2<Long, TimerTask> tt : timers) {
			long delay = tt._1();
			TimerTask task = tt._2();
			runtimeInterface.scheduleTask(delay, task);
		}
	}

	private Random rng;

	public void launchProtocolRandomGraph(int n, int degree,
			ProtocolInstanceFactory factory) {
		launchProtocol(factory, new RandomGraph(n, defaultAddressFunc,
				degree, rng));
	}

	public void launchProtocolCompleteGraph(int n,
			ProtocolInstanceFactory factory) {
		launchProtocol(factory, new CompleteGraph(n, defaultAddressFunc));
	}

	private List<Runtime> runtimes;

	public List<Runtime> getRuntimes() {
		return runtimes;
	}

	private void run() {
		runtimeInterface.run();
		System.out.println("Done");
	}

	public void runGraph(ProtocolInstanceFactory factory,
			TestHarnessGraph graph) {
		launchProtocol(factory, graph);
		run();
	}


	public void stop() {
		runtimeInterface.stop();
	}

	/**
	 * Implementations of this interface create protocol instances for the
	 * TestHarness
	 * 
	 * @author lonnie
	 * 
	 * @param 
	 */
	public static interface ProtocolInstanceFactory {
		public Protocol createProtocolInstance(int nodeId, Address address,
				Overlay overlay);
	};

	// backwards compatibility method; do not use
	public static  ProtocolInstanceFactory factoryFromCNF(
			final F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
		return new ProtocolInstanceFactory() {
			@Override
			public Protocol createProtocolInstance(int nodeId, Address address,
					Overlay overlay) {
				return createNodeFunc.f(nodeId, address, overlay);
			}
		};
	}

	// backwards compatibility
	public static  void main(String[] argv,
			F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
		TestHarness.main(argv, TestHarness.factoryFromCNF(createNodeFunc));
	}

	public static  void main(String[] argv,
			ProtocolInstanceFactory factory) {
		TestHarness harness = new TestHarness();
		harness.runMain(argv, factory);
	}

	public MicaOptions defaultOptions() {
		return new MicaOptions();
	}

	public MicaOptions parseOptions(String[] argv) {
		MicaOptions options = defaultOptions();
		new JCommander(options, argv); // parse command line options
		return options;
	}

	public void runMain(String[] argv, ProtocolInstanceFactory factory) {
		MicaOptions options = parseOptions(argv);
		runMain(options, factory);
	}

	public void runMain(String[] argv) {
		// will throw an invalid cast exception of this harness doesn't implement ProtocolInstanceFactory
		ProtocolInstanceFactory factory = (ProtocolInstanceFactory) this;
		MicaOptions options = parseOptions(argv);
		runMain(options, factory);
	}

	public void runMain(String[] argv,
			F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
		runMain(argv, TestHarness.factoryFromCNF(createNodeFunc));
	}

	/**
	 * Instantiate nodes and create RunTime instances.
	 * 
	 * Legacy method for backwards compatibility. Use runGraph directly or
	 * runMain.
	 * 
	 * @param seed
	 * @param n
	 * @param nodeDegree
	 * @param createNodeFunc
	 */
	public void runRandomGraph(long seed, int n, int nodeDegree,
			ProtocolInstanceFactory factory) {
		rng = new Random(seed);
		TestHarnessGraph graph = new RandomGraph(n, defaultAddressFunc,
				nodeDegree, rng);
		runGraph(factory, graph);
	}

	private TestHarnessGraph graph = null;

	public TestHarnessGraph getGraph() {
		return graph;
	}

	public void setGraph(TestHarnessGraph graph) {
		this.graph = graph;
	}

	private MicaOptions options = null;

	public MicaOptions getOptions() {
		return options;
	}

	private void setOptions(MicaOptions options) {
		this.options = options;	
		// validate options and do option processing...
		String runtimeName = options.implementation;
		if(runtimeName.equals("simple")) {
			runtimeInterface = new SimpleRuntimeInterface();
		} else if(runtimeName.equals("sim")) {
			runtimeInterface = Simulator.v();
		} else if(runtimeName.equals("a1")) {
			runtimeInterface = new A1RuntimeInterface();
		}
		runtimeInterface.reset();
		MiCA.setRuntimeInterface(runtimeInterface);
		MiCA.setOptions(options);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processOptions() {
		MicaOptions options = getOptions();
		options.mainClassName = this.getClass().getName();
		
		if (options.stopAfter > 0) {
			addTimerRounds(options.stopAfter, taskStop());
		}

		// initialize random number generator
		rng = new Random(options.seed);

		F<Integer, Address> addressFunc = runtimeInterface.getAddressFunc();
				
		if(addressFunc == null) {
			addressFunc = defaultAddressFunc;
		}
		
		List<Address> addresses = Functional.list(Functional.map(
				Functional.range(options.n), addressFunc));

		// initialize communications graph
		if (options.graphType.equals("random")) {
			setGraph(new RandomGraph(addresses, options.rdegree, rng));
		} else if (options.graphType.equals("complete")) {
			setGraph(new CompleteGraph(addresses));
		} else if (options.graphType.equals("singlering")) {
			setGraph(new SinglyLinkedRingGraph(addresses));
		}

		// initialize log
		if (options.clearLogdir) {
			clearLogdir();
		}
		
		// logging options
		LogFlag.setCurrentLogMask(LogFlag.set(LogFlag.getCurrentLogMask(), (List) options.logsEnable));
		LogFlag.setCurrentLogMask(LogFlag.unset(LogFlag.getCurrentLogMask(), (List) options.logsDisable));

	}

	/**
	 * Delete any files in the logdir directory ending with the suffix ".log"
	 */
	public void clearLogdir() {
		File logdir = new File(options.logdir);
		if (!logdir.exists())
			return;
		if (!logdir.isDirectory())
			throw new RuntimeException(String.format(
					"Logdir %s not a directory", logdir));

		for (String logfilename : logdir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".log");
			}
		})) {
			File f = new File(logdir, logfilename);
			f.delete();
		}
	}

	public void runMain(MicaOptions options,
			ProtocolInstanceFactory factory) {
		assert (options != null);
		setOptions(options);
		// SimpleRuntime.DEFAULT_INTERVAL = (int) options.roundLength;
		TestHarness.BASE_ADDRESS = options.port;

		processOptions();

		if (graph == null) {
			throw new RuntimeException(
					"Invalid graph.  graphType options \"complete\" and \"random\"");
		}

		runGraph(factory, graph);
	}

}
