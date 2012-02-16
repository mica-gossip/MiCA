package org.princehouse.mica.util;

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
		 
		  @Parameter(names = { "-log" }, description = "Log file location")
		  public String logfile = "mica.log";
		 
		  @Parameter(names = "-n", description = "Number of nodes to run")
		  public Integer n = 25;
		 
		  @Parameter(names = "-rdegree", description = "Degree of nodes in random graph. (Currently must be even)")
		  public Integer rdegree = 4;
		  
		  @Parameter(names = "-port", description = "Starting port")
		  public Integer port = 8000;
		  
		  @Parameter(names = "-host", description = "Host")
		  public String host = "localhost";
		  
		  @Parameter(names = "-seed", description = "Random seed")
		  public Long seed = 0L;
		  
		  @Parameter(names = "-round", description = "Round length (ms)")
		  public long roundLength = 1000L;
		  
		  @Parameter(names = "-stopAfter", description = "Halt simulation after this many rounds (0 = run forever)")
		  public double stopAfter = 0;
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
	
	public List<Runtime<Q>> launchProtocol(int n,
			F3<Integer, Address, List<Address>, Q> createNodeFunc,
			F<Integer, List<Integer>> neighborsFunc,
			F<Integer, Address> addressFunc) {

		List<Runtime<Q>> runtimes = Functional.list();

		Runtime.log(String.format("-,-,-,init_experiment,round_ms=%d nodes=%d",SimpleRuntime.DEFAULT_INTERVAL, n));
		running = true;
		
		launchTimers();
		
		for (int i = 0; i < n; i++) {
			try {
				// Stagger protocol launching by a few milliseconds
				Thread.sleep(random.nextInt(50));
			} catch (InterruptedException e) {
			}

			Address address = addressFunc.f(i);
			List<Address> neighbors = Functional.list(Functional.map(
					neighborsFunc.f(i), addressFunc));
			Q pinstance = createNodeFunc.f(i, address, neighbors);
			Runtime<Q> rt = SimpleRuntime.launchDaemon(pinstance, address);
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
	
	private Random random;

	public List<Runtime<Q>> launchProtocolRandomGraph(int n, int degree,
			F3<Integer, Address, List<Address>, Q> createNodeFunc) {
		RandomGraph rg = new RandomGraph(n, degree, random);
		return launchProtocol(n, createNodeFunc, rg.getNeighbors(),
				defaultAddressFunc);
	}

	private List<Runtime<Q>> runtimes;

	// public static <P extends Protocol> List<Runtime<P>> launch

	public List<Runtime<Q>> getRuntimes() {
		return runtimes;
	}
	
	public void runRandomGraph(long seed, int n, int degree,
			F3<Integer, Address, List<Address>, Q> createNodeFunc) {
		random = new Random(seed);
		runtimes = launchProtocolRandomGraph(n, degree, createNodeFunc);
		
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
	
	public static <ProtocolClass extends Protocol> void main(String[] argv, F3<Integer, Address, List<Address>, ProtocolClass> createNodeFunc) {
		TestHarness<ProtocolClass> harness = new TestHarness<ProtocolClass>();
		harness.runMain(argv, createNodeFunc);
	}

	public void runMain(String[] argv, F3<Integer, Address, List<Address>, Q> createNodeFunc) {
		TestHarnessOptions options = new TestHarnessOptions();
		new JCommander(options, argv); // parse command line options
		
		SimpleRuntime.DEFAULT_INTERVAL = (int) options.roundLength;
		TestHarness.BASE_ADDRESS = options.port;
		//TestHarness<ProtocolClass> harness = new TestHarness<ProtocolClass>();
		if(options.stopAfter > 0) {
			addTimerRounds(options.stopAfter, taskStop());
		}
		runRandomGraph(options.seed, options.n, options.rdegree, createNodeFunc);
	}

}
