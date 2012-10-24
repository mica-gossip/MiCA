package org.princehouse.mica.util.harness;

import java.io.File;
import java.io.FilenameFilter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.SinglyLinkedRingGraph;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import fj.F;
import fj.F3;
import fj.P;
import fj.P2;

/**
 * TestHarness is used for running local experiments with many nodes on a randomly-generated graph
 * See examples.DemoCompositeProtocol for an example
 * 
 * Command line options for TestHarness are in the TestHarnessOptions inner class
 * @author lonnie
 *
 * @param <Q>
 */
public class TestHarness<Q extends Protocol> {

	public static class TestHarnessOptions {

		@Parameter(names = { "-log" }, description = "CSV Log file location (deprecated)")
		public String logfile = "mica.log";

		@Parameter(names = {"-logdir"}, description = "JSON log directory.  Default ./mica_log")
		public String logdir = "mica_log"; 

		@Parameter(names = {"-clearlogdir"}, description = "Delete pre-existing log files in logdir")
		public Boolean clearLogdir = true;

		@Parameter(names = "-n", description = "Number of nodes to run")
		public Integer n = 25;

		@Parameter(names = "-rdegree", description = "Degree of nodes in random graph. (Currently must be even).  Only used for graphType=random")
		public Integer rdegree = 4;

		@Parameter(names = "-port", description = "Starting port")
		public Integer port = 8000;

		@Parameter(names = "-host", description = "Host")
		public String host = "localhost";

		@Parameter(names = "-seed", description = "Random seed")
		public Long seed = 0L;

		@Parameter(names = "-round", description = "Round length (ms)")
		public int roundLength = SimpleRuntime.DEFAULT_INTERVAL;

		@Parameter(names = "-stopAfter", description = "Halt simulation after this many rounds (0 = run forever)")
		public double stopAfter = 0;

		@Parameter(names = "-graphType", description = "Type of communication graph to use. Valid options: random, complete, singlering")
		public String graphType = "random";

	}



	private List<P2<Long,TimerTask>> timers = Functional.list();

	public void addTimer(long time, TimerTask task) {
		timers.add(P.p(time,task));
	}

	private int getRoundMS() {
		return SimpleRuntime.DEFAULT_INTERVAL;
	}

	public void addTimerRounds(double rounds, TimerTask task) {
		addTimer((long) (rounds * getRoundMS()), task );
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

	public TimerTask taskStop() {
		final TestHarness<Q> harness = this;
		return new TimerTask() {
			@Override
			public void run() {
				harness.stop();			
				Runtime.debug.println("End timer!");
			}
		};
	}


	public List<Runtime<Q>> launchProtocol(
			ProtocolInstanceFactory<Q> factory,
			TestHarnessGraph g) {

		List<Runtime<Q>> runtimes = Functional.list();
	
		running = true;

		launchTimers();

		List<Address> addresses = g.getAddresses();
		
		int i = 0;
		for (Address addr : addresses) {
			try {
				// Stagger protocol launching by a few milliseconds
				Thread.sleep(rng.nextInt(50));
			} catch (InterruptedException e) {
			}

			Overlay neighbors = g.getOverlay(addr);
			
			// FIXME need to associate runtime right here
			SimpleRuntime<Q> rt = new SimpleRuntime<Q>(addr);
			Runtime.setRuntime(rt);
			Q pinstance = factory.createProtocolInstance(i++, addr, neighbors);
			SimpleRuntime.launchDaemon(rt, pinstance, addr, getOptions().roundLength, getOptions().seed);
			Runtime.setRuntime(null);
			
			runtimes.add(rt);
			
		}

		return runtimes;
	}

	private void launchTimers() {
		Timer timer = new Timer(true);
		for(P2<Long,TimerTask> tt : timers) {
			long delay = tt._1();
			TimerTask task = tt._2();
			timer.schedule(task,delay);
		}
	}

	private Random rng;


	public List<Runtime<Q>> launchProtocolRandomGraph(int n, int degree,
			ProtocolInstanceFactory<Q> factory) {
		return launchProtocol(
				factory, 
				new RandomGraph(n, defaultAddressFunc, degree, rng));
		}

	public List<Runtime<Q>> launchProtocolCompleteGraph(int n, ProtocolInstanceFactory<Q> factory) {
		return launchProtocol(factory, new CompleteGraph(n, defaultAddressFunc));
	}

	private List<Runtime<Q>> runtimes;


	public List<Runtime<Q>> getRuntimes() {
		return runtimes;
	}

	public void runGraph( 
			ProtocolInstanceFactory<Q> factory,
			TestHarnessGraph graph
			) {

		runtimes = launchProtocol(factory, graph);

		// wait for interrupt
		try {
			try {
				while (running) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {

			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		stopRuntimes();
		System.out.println("Done");
	}

	private boolean running = false;

	public void stopRuntimes() {
		running = false;
		for (Runtime<Q> rt : runtimes) {
			rt.stop();
		}
	}

	public void stop() {
		stopRuntimes();
	}

	/**
	 * Implementations of this interface create protocol instances for the TestHarness
	 * 
	 * @author lonnie
	 *
	 * @param <Q>
	 */
	public static interface ProtocolInstanceFactory<Q extends Protocol> {
		public Q createProtocolInstance(int nodeId, Address address, Overlay overlay);		
	};
	
	// backwards compatibility method; do not use
	public static <R extends Protocol> ProtocolInstanceFactory<R> factoryFromCNF(final F3<Integer, Address, Overlay, R> createNodeFunc) {
		return new ProtocolInstanceFactory<R>() {
			@Override
			public R createProtocolInstance(int nodeId, Address address,
					Overlay overlay) {
				return createNodeFunc.f(nodeId, address, overlay);
			}
		};
	}
	
	// backwards compatibility
	public static <ProtocolClass extends Protocol> void main(String[] argv, F3<Integer, Address, Overlay, ProtocolClass> createNodeFunc) {
		TestHarness.main(argv, TestHarness.factoryFromCNF(createNodeFunc));
	}

	public static <ProtocolClass extends Protocol> void main(String[] argv, ProtocolInstanceFactory<ProtocolClass> factory) {
		TestHarness<ProtocolClass> harness = new TestHarness<ProtocolClass>();
		harness.runMain(argv, factory);
	}
	
	public static TestHarnessOptions defaultOptions() {
		return new TestHarnessOptions();
	}

	public static TestHarnessOptions parseOptions(String[] argv) {
		TestHarnessOptions options = defaultOptions();
		new JCommander(options, argv); // parse command line options
		return options;
	}
	
	public void runMain(String[] argv, ProtocolInstanceFactory<Q> factory) {
		TestHarnessOptions options = parseOptions(argv);
		runMain(options, factory);
	}
	
	
	

	public void runMain(String[] argv, F3<Integer, Address, Overlay, Q> createNodeFunc) {
		runMain(argv, TestHarness.factoryFromCNF(createNodeFunc));
	}
	
	/**
	 * Instantiate nodes and create RunTime instances. 
	 * 
	 * Legacy method for backwards compatibility. Use runGraph directly or runMain.
	 * @param seed
	 * @param n
	 * @param nodeDegree
	 * @param createNodeFunc
	 */
	public void runRandomGraph(long seed, int n, int nodeDegree,
			ProtocolInstanceFactory<Q> factory) {
		rng = new Random(seed);
		TestHarnessGraph graph = new RandomGraph(n,defaultAddressFunc, nodeDegree,rng);
		runGraph(factory, graph);
	}

	private TestHarnessGraph graph = null;

	public TestHarnessGraph getGraph() {
		return graph;
	}

	public void setGraph(TestHarnessGraph graph) {
		this.graph = graph;
	}

	private TestHarnessOptions options = null;

	public TestHarnessOptions getOptions() {
		return options;
	}

	public void setOptions(TestHarnessOptions options) {
		this.options = options;
	}

	public void processOptions() {
		TestHarnessOptions options = getOptions();

		if(options.stopAfter > 0) {
			addTimerRounds(options.stopAfter, taskStop());
		}

		// initialize random number generator
		rng = new Random(options.seed);

		F<Integer,Address> addressFunc = defaultAddressFunc;
		List<Address> addresses = Functional.list(Functional.map(Functional.range(options.n), addressFunc));
		
		// initialize communications graph
		if(options.graphType.equals("random")) {
			setGraph(new RandomGraph(addresses, options.rdegree, rng));
		} else if(options.graphType.equals("complete")) {
			setGraph(new CompleteGraph(addresses));
		} else if(options.graphType.equals("singlering")) {
			setGraph(new SinglyLinkedRingGraph(addresses));
		}

		// initialize log
		if(options.clearLogdir) {
			clearLogdir();
		}
	}


	/**
	 * Delete any files in the logdir directory ending with the suffix ".log"
	 */
	public void clearLogdir() {
		File logdir = new File(options.logdir);
		if(!logdir.exists())
			return;
		if(!logdir.isDirectory()) 
			throw new RuntimeException(String.format("Logdir %s not a directory",logdir));

		for(String logfilename : logdir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".log");
			}
		})) {
			File f = new File(logdir,logfilename);
			f.delete();
		}
	}

	public void runMain(TestHarnessOptions options, ProtocolInstanceFactory<Q> factory) {
		assert(options != null);
		setOptions(options);
		//SimpleRuntime.DEFAULT_INTERVAL = (int) options.roundLength;
		TestHarness.BASE_ADDRESS = options.port;

		processOptions();

		if(graph == null) {
			throw new RuntimeException("Invalid graph.  graphType options \"complete\" and \"random\"");
		}

		runGraph(factory, graph);
	}

}
