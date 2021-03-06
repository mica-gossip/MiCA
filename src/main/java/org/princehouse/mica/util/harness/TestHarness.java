package org.princehouse.mica.util.harness;

import com.beust.jcommander.JCommander;
import fj.F;
import fj.F3;
import fj.P;
import fj.P2;
import java.io.File;
import java.io.FilenameFilter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.exceptions.InvalidOption;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeInterface;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.sim.FakeCompiler;
import org.princehouse.mica.base.sim.Simulator;
import org.princehouse.mica.base.simple.SimpleCompiler;
import org.princehouse.mica.base.simple.SimpleRuntimeInterface;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Array;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.SinglyLinkedRingGraph;

/**
 * TestHarness is used for running local experiments with many nodes on a randomly-generated graph
 * See org.princehouse.mica.example.RunCompositiveProtocol for example usage
 * <p>
 * Command line options for TestHarness are in the MicaOptions class
 *
 * @param
 * @author lonnie
 */
public class TestHarness implements ProtocolInstanceFactory {

  private RuntimeInterface runtimeInterface = null;
  public static final String LOG_NAMES = Array.join(", ", LogFlag.values());
  private List<P2<Long, TimerTask>> timers = Functional.list();

  public void addTimer(long time, TimerTask task) {
    timers.add(P.p(time, task));
  }

  private int getRoundMS() {
    if (getOptions() == null) {
      throw new RuntimeException(
          "Test Harness options must be set prior to calling getRoundMS().  Use parseOptions + setOptions and then call runMain with no arguments");
    }
    return getOptions().roundLength;
  }

  public void addTimerRounds(double rounds, TimerTask task) {
    addTimer((long) (rounds * getRoundMS()), task);
  }

  public static int BASE_PORT = 8000;
  public static F<Integer, Address> defaultAddressFunc = new F<Integer, Address>() {
    public Address f(Integer i) {
      try {
        return TCPAddress.valueOf(String.format("localhost:%d", BASE_PORT + i));
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }
    }
  };

  private Random random = null;

  public Random getRandom() {
    if (random == null) {
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
        MicaRuntime.debug.println("End timer!");
      }
    };
  }

  public void launchProtocol() {

    TestHarnessGraph g = getGraph();
    launchTimers(); // TODO lift to runtime interface

    int i = 0;

    for (Address addr : g.getAddresses()) {
      Overlay neighbors = g.getOverlay(addr);
      MicaOptions options = getOptions();
      int stagger = rng.nextInt(options.stagger);
      int lockTimeout = options.timeout;
      long seed = getRandom().nextLong();
      MicaRuntime rt = runtimeInterface
          .addRuntime(addr, seed, options.roundLength, stagger, lockTimeout);

      MiCA.getRuntimeInterface().getRuntimeContextManager().setNativeRuntime(rt);
      Protocol pinstance = createProtocolInstance(i++, addr, neighbors);
      MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
      rt.setProtocolInstance(pinstance);
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

  /*
   * public void launchProtocolRandomGraph(int n, int degree,
   * ProtocolInstanceFactory factory) { launchProtocol(factory, new
   * RandomGraph(n, defaultAddressFunc, degree, rng)); }
   */

  /*
   * public void launchProtocolCompleteGraph(int n, ProtocolInstanceFactory
   * factory) { launchProtocol(factory, new CompleteGraph(n,
   * defaultAddressFunc)); }
   */

  public List<MicaRuntime> getRuntimes() {
    return MiCA.getRuntimeInterface().getRuntimes();
  }

  private void run() {
    runtimeInterface.run();
    System.out.println("Done");
  }

  public void runGraph() {
    launchProtocol(); // getFactory(), getGraph());
    run();
  }

  public void stop() {
    runtimeInterface.stop();
  }

  // backwards compatibility method; do not use
  public static ProtocolInstanceFactory factoryFromCNF(
      final F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
    return new ProtocolInstanceFactory() {
      @Override
      public Protocol createProtocolInstance(int nodeId, Address address, Overlay overlay) {
        return createNodeFunc.f(nodeId, address, overlay);
      }
    };
  }

  // backwards compatibility
  public static void main(String[] argv, F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
    TestHarness.main(argv, TestHarness.factoryFromCNF(createNodeFunc));
  }

  public static void main(String[] argv, ProtocolInstanceFactory factory) {
    TestHarness harness = new TestHarness();
    harness.runMain(argv, factory);
  }

  public MicaOptions defaultOptions() {
    return new MicaOptions();
  }

  public MicaOptions parseOptions(String[] argv) {
    MicaOptions options = defaultOptions();
    new JCommander(options, argv); // parse command line options
    validateOptions(options);
    return options;
  }

  public void validateOptions(MicaOptions options) {
    if (options.serializer.equals("kryo")) {
      throw new UnsupportedOperationException("kryo serializer no longer supported");
    }
  }

  public void runMain(String[] argv, ProtocolInstanceFactory factory) {
    MicaOptions options = parseOptions(argv);
    runMain(options, factory);
  }

  public void runMain(String[] argv) {
    // will throw an invalid cast exception of this harness doesn't
    // implement ProtocolInstanceFactory
    ProtocolInstanceFactory factory = this;
    MicaOptions options = parseOptions(argv);
    assert (options != null);
    runMain(options, factory);
  }

  public void runMain() {
    if (getOptions() == null) {
      throw new RuntimeException(
          "Options must be set with parseOptions + setOptions prior to using this constructor");
    }
    runMain(getOptions(), this);
  }

  public void runMain(String[] argv, F3<Integer, Address, Overlay, Protocol> createNodeFunc) {
    runMain(argv, TestHarness.factoryFromCNF(createNodeFunc));
  }

  /**
   * Instantiate nodes and create RunTime instances.
   * <p>
   * Legacy method for backwards compatibility. Use runGraph directly or runMain.
   *
   * @param seed
   * @param n
   * @param nodeDegree
   * @param createNodeFunc
   */
  /*
   * public void runRandomGraph(long seed, int n, int nodeDegree,
   * ProtocolInstanceFactory factory) { rng = new Random(seed);
   * TestHarnessGraph graph = new RandomGraph(n, defaultAddressFunc,
   * nodeDegree, rng); runGraph(factory, graph); }
   */

  private TestHarnessGraph graph = null;

  public TestHarnessGraph getGraph() {
    return graph;
  }

  public void setGraph(TestHarnessGraph graph) {
    this.graph = graph;
  }

  private MicaOptions options = null;

  public MicaOptions getOptions() {
    if (options == null) {
      throw new RuntimeException(
          "TestHarness not initialized. Options must be set before calling this");
    }
    return options;
  }

  private void setOptions(MicaOptions options) {
    this.options = options;
    // validate options and do option processing...
    String runtimeName = options.implementation;
    if (runtimeName.equals("simple")) {
      runtimeInterface = new SimpleRuntimeInterface();
    } else if (runtimeName.equals("simulation")) {
      runtimeInterface = Simulator.v();
    } else {
      throw new InvalidOption("implementation", options.implementation);
    }

    if (options.compiler.equals("default")) {
      MiCA.setCompiler(runtimeInterface.getDefaultCompiler());
    } else if (options.compiler.equals("simple")) {
      MiCA.setCompiler(new SimpleCompiler());
    } else if (options.compiler.equals("fake")) {
      MiCA.setCompiler(new FakeCompiler());
    } else {
      throw new InvalidOption("compiler", options.compiler);
    }

    runtimeInterface.reset();
    MiCA.setRuntimeInterface(runtimeInterface);
    MiCA.setOptions(options);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void processOptions() {
    MicaOptions options = getOptions();
    options.mainClassName = this.getClass().getName();

    if (options.stopAfter > 0) {
      addTimerRounds(options.stopAfter, taskStop());
    }

    // initialize random number generator
    rng = new Random(options.seed);

    F<Integer, Address> addressFunc = runtimeInterface.getAddressFunc();

    if (addressFunc == null) {
      addressFunc = defaultAddressFunc;
    }

    List<Address> addresses = Functional
        .list(Functional.map(Functional.range(options.n), addressFunc));

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
    LogFlag
        .setCurrentLogMask(LogFlag.unset(LogFlag.getCurrentLogMask(), (List) options.logsDisable));

    TestHarness.BASE_PORT = options.port;

    if (getGraph() == null) {
      throw new RuntimeException("Invalid graph.  graphType options \"complete\" and \"random\"");
    }
  }

  /**
   * Delete any files in the logdir directory ending with the suffix ".log"
   */
  public void clearLogdir() {
    File logdir = new File(options.logdir);
    if (!logdir.exists()) {
      return;
    }
    if (!logdir.isDirectory()) {
      throw new RuntimeException(String.format("Logdir %s not a directory", logdir));
    }

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

  public void runMain(MicaOptions options, ProtocolInstanceFactory factory) {
    assert (options != null);
    setOptions(options);
    setFactory(factory);
    processOptions();
    configure();
    runGraph();
  }

  /**
   * Perform experiment-specific setup after options are processed but before execution
   */
  public void configure() {
  }

  public void runMain(MicaOptions options) {
    runMain(options, this);
  }

  private ProtocolInstanceFactory factory = null;

  public ProtocolInstanceFactory getFactory() {
    return factory;
  }

  public void setFactory(ProtocolInstanceFactory factory) {
    this.factory = factory;
  }

  @Override
  public Protocol createProtocolInstance(int nodeId, Address address, Overlay overlay) {
    return factory.createProtocolInstance(nodeId, address, overlay);
  }

}
