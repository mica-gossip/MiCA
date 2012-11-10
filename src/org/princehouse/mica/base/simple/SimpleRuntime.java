package org.princehouse.mica.base.simple;

import static org.princehouse.mica.base.RuntimeErrorCondition.ACTIVE_GOSSIP_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.BIND_ADDRESS_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.INITIATOR_LOCK_TIMEOUT;
import static org.princehouse.mica.base.RuntimeErrorCondition.NULL_SELECT;
import static org.princehouse.mica.base.RuntimeErrorCondition.OPEN_CONNECTION_FAIL;
import static org.princehouse.mica.base.RuntimeErrorCondition.POSTUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.PREUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.SELECT_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.SELF_GOSSIP;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.MalformedViewException;
import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.net.model.NotBoundException;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Logging;
import org.princehouse.mica.util.WeakHashSet;

/**
 * Basic Runtime implementation.
 * 
 * Nothing fancy: It just serializes and exchanges complete node state.
 * 
 */
public class SimpleRuntime<P extends Protocol> extends Runtime<P> implements
		AcceptConnectionHandler {

	
	public static final boolean DEBUG_NETWORKING = false;

	private ReentrantLock lock = new ReentrantLock();

	public static int DEFAULT_INTERVAL = 1500; // 1.5 seconds
	private static long LOCK_WAIT_MS = DEFAULT_INTERVAL;

	public static long DEFAULT_RANDOM_SEED = 0L;

	public Address address;

	public SimpleRuntime(Address address) {
		super();
		setAddress(address);
	}

	public void setAddress(Address address) {
		if (this.address != null && !this.address.equals(address)) {
			throw new RuntimeException(
					"previous address non-null; cannot change the address of an existing runtime");
		}
		this.address = address;
		runtimeState.setAddress(address);
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread. Uses
	 * SimpleRuntime.DEFAULT_INTERVAL as the gossip interval and
	 * SimpleRuntime.DEFAULT_RANDOM_SEED as the random seed.
	 * 
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @param daemon
	 *            Launch thread as a daemon
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launch(Runtime<T> rt,
			final T pinstance, final boolean daemon) {
		return launch(rt, pinstance, daemon, DEFAULT_INTERVAL,
				DEFAULT_RANDOM_SEED);
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread.
	 * 
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @param daemon
	 *            Launch thread as a daemon
	 * @param intervalMS
	 *            Milliseconds to sleep between each gossip initiation
	 * @param randomSeed
	 *            Random seed to use for this runtime
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launch(final Runtime<T> rt,
			final T pinstance, final boolean daemon, final int intervalMS,
			final long randomSeed) {
		Thread t = new Thread() {
			public void run() {
				try {
					rt.run(pinstance, intervalMS, randomSeed);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(daemon);
		t.start();
		return rt;
	}

	/**
	 * COMPAT MODE --- DO NOT USE should explicitly create a runtime and call
	 * Runtime.setRuntime BEFORE creating protocol instance
	 * 
	 * @param pinstance
	 * @param address
	 * @return
	 */
	@Deprecated
	public static <T extends Protocol> Runtime<T> launchDaemon(
			final T pinstance, Address address) {
		Runtime<T> rt = new SimpleRuntime<T>(address);
		return launch(rt, pinstance, true);
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread. (Calls
	 * through to launch(), with the daemon flag true)
	 * 
	 * @param rt
	 *            SimpleRuntime instance -- must be created prior to protocol
	 *            instance creation
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launchDaemon(Runtime<T> rt,
			final T pinstance) {
		return launch(rt, pinstance, true);
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread. (Calls
	 * through to launch(), with the daemon flag true)
	 * 
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @param intervalMS
	 *            milliseconds to sleep between each gossip initiation
	 * @param randomSeed
	 *            random seed to be used for this runtime
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launchDaemon(
			SimpleRuntime<T> rt, final T pinstance, final Address address,
			int intervalMS, long randomSeed) {
		return launch(rt, pinstance, true, intervalMS, randomSeed);
	}

	private P pinstance;

	@Override
	public void acceptConnection(Address recipient, Connection connection)
			throws IOException {
		try {
			if (lock.tryLock(LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {
				if (!running) {
					logJson("mica-error-internal",
							"acceptConnection called on a stopped runtime");
					connection.close();
					return;
				}
				setRuntime(this);
				((SimpleRuntimeAgent<P>) compile(pinstance)).acceptConnection(
						this, getProtocolInstance(), connection);
				clearRuntime(this);
				lock.unlock();
			} else {
				if (!running) {
					logJson("mica-error-internal",
							"acceptConnection called on a stopped runtime + lock failed");
					connection.close();
					return;
				}
				// failed to acquire lock; timeout
				logJson("mica-error-accept-connection");
				System.err.printf(
						"%s accept: failed to acquire lock (timeout)\n", this);
				connection.close();
			}
		} catch (InterruptedException e) {
			try {
				handleError(RuntimeErrorCondition.INTERRUPTED);
			} catch (FatalErrorHalt e1) {
			} catch (AbortRound e1) {
			}
		}
	}

	private boolean running = true;

	@Override
	public void run(P pinstance, int intervalMS, long randomSeed)
			throws InterruptedException {

		final Address address = getAddress();

		super.run(pinstance, intervalMS, randomSeed);

		// Initialize RuntimeState available to protocols
		runtimeState.setAddress(address);
		runtimeState.setRandom(new Random(randomSeed));
		runtimeState.setIntervalMS(intervalMS);

		setProtocolInstance(pinstance);

		Random rng = new Random(randomSeed);

		logState("initial");

		try {
			address.bind(this);
		} catch (IOException e1) {
			logJson("mica-error-internal", e1);
			try {
				handleError(BIND_ADDRESS_EXCEPTION);
			} catch (FatalErrorHalt e) {
				return;
			} catch (AbortRound e) {
				return; // same as fatal in this case
			}
		}

		long lastElapsedMS = 0L;

		double rate = getRate(getProtocolInstance());

		try {
			while (running) {

				Connection connection = null;
				long startTime = getTimeMS();
				Address partner = null;

				try {
					logJson("mica-rate", rate);

					int intervalLength = (int) (((double) intervalMS) / rate);
					if (intervalLength <= 0) {
						System.err
								.printf("%s error: Rate * intervalMS <= 0.  Resetting to default.\n",
										this);
						intervalLength = intervalMS;
					}
					Thread.sleep(Math.max(0L, intervalLength - lastElapsedMS));
					if (!running)
						break;

					if (lock.tryLock(LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {

						if (!running) {
							// recv thread may have shutdown while it held
							// the lock.
							// now that we have it, test for this
							lock.unlock();
							break;
						}
						RuntimeAgent<P> agent = compile(getProtocolInstance());

						Logging.SelectEvent se = null;
						try {
							se = agent.select(this,
									getProtocolInstance(), rng.nextDouble());
						} catch (SelectException e) {
							handleError(SELECT_EXCEPTION,e);
						}

						partner = se.selected;


						logJson("mica-select", se);

						try {
							// preUpdate is called even if partner is
							// invalid
							// (null or self address)
							getProtocolInstance().preUpdate(partner);
						} catch (Throwable t) {
							handleError(PREUDPATE_EXCEPTION, t);
						}
						logState("preupdate");

						if (getAddress().equals(partner)) {
							handleError(SELF_GOSSIP);
						} else if (partner == null) {
							handleError(NULL_SELECT);
						}

						try {
							connection = partner.openConnection();
						} catch (ConnectException ce) {
							handleError(OPEN_CONNECTION_FAIL, ce);
						} catch (IOException io) {
							handleError(OPEN_CONNECTION_FAIL, io);
						}

						try {
							agent.gossip(this, getProtocolInstance(),
									connection);
						} catch (AbortRound ar) {
							throw ar;
						} catch (FatalErrorHalt feh) {
							throw feh;
						} catch (Throwable t) {
							// May be a serialization problem!
							handleError(ACTIVE_GOSSIP_EXCEPTION, t);
						}

						logState("gossip-initiator");

						try {
							getProtocolInstance().postUpdate();
						} catch (Throwable t) {
							handleError(POSTUDPATE_EXCEPTION, t);
						}
						logState("postupdate");

						getRuntimeState().incrementRound();
						rate = getRate(getProtocolInstance());
						lock.unlock();
					} else {
						// failed to acquire lock within time limit; gossip
						handleError(INITIATOR_LOCK_TIMEOUT);
					}
				} catch (AbortRound ar) {
					// close connection, if applicable
					if (connection != null) {
						try {
							connection.close();
						} catch (IOException e) {}
					}
					try {
						lock.unlock(); // try to release lock
					} catch (IllegalMonitorStateException ie) {}
				}
				lastElapsedMS = getTimeMS() - startTime;
				double sec = ((double)lastElapsedMS)/1000.0;
				Runtime.debug.printf("%s -> %s, elapsed time %g s\n", this, partner, sec);
			}
		} catch (FatalErrorHalt e) {
			// fatalErrorHalt should have already shut down everything
		} // end while(running) loop
	}

	
	@Override
	protected void tolerateError() throws AbortRound {
		throw new AbortRound();
	}

	@Override
	protected void fatalErrorHalt(RuntimeErrorCondition condition)
			throws FatalErrorHalt {
		stop(); // passively signal that it's time to shut down
		try {
			address.unbind();// try to unbind the listener
		} catch (NotBoundException e1) {
		}
		try {
			lock.unlock();
		} catch (IllegalMonitorStateException e) {
		}
		throw new FatalErrorHalt();
	}

	@Override
	public String toString() {
		return String.format("<rt %s>", getAddress());
	}

	private long getTimeMS() {
		return new Date().getTime();
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public P getProtocolInstance() {
		return pinstance;
	}

	@Override
	public void setProtocolInstance(P pinstance) {
		this.pinstance = pinstance;
	}

	@Override
	public Address getAddress() {
		return address;
	}

	// ---- agent execution context utilities ------------
	private WeakHashSet<Object> foreignObjects = null;
	private RuntimeState foreignState = null;

	protected void setForeignState(WeakHashSet<Object> foreignObjects,
			RuntimeState foreignState) {
		// only knows about foreign BaseProtocol subclasses for now
		this.foreignObjects = foreignObjects;
		this.foreignState = foreignState;
	}

	protected void clearForeignState() {
		foreignObjects = null;
		foreignState = null;
	}

	// ---------------------------------------------------

	private RuntimeState runtimeState = new RuntimeState();

	@Override
	public RuntimeState getRuntimeState(Protocol p) {
		if (foreignObjects != null) {
			if (foreignObjects.contains(p))
				return foreignState;
		}
		return runtimeState;
	}

	@Override
	public RuntimeState getRuntimeState() {
		return runtimeState;
	}

	@Override
	public Distribution<Address> getView(Protocol protocol)
			throws SelectException {

		Distribution<Address> view = compile(protocol).getView(this, protocol);

		if (view != null && view.isEmpty())
			return null;

		if (view != null && !view.isOne()) {
			throw new MalformedViewException(protocol, view);
		}

		return view;
	}

	@Override
	public double getRate(Protocol protocol) {
		return compile(protocol).getRate(this, protocol);
	}

	@Override
	public void executeUpdate(Protocol p1, Protocol p2) {
		// TODO sanity check that p1, p2 are same types, or have a common parent
		// class
		SimpleRuntimeAgent<Protocol> agent = (SimpleRuntimeAgent<Protocol>) compile(p1);
		agent.executeUpdate(this, p1, p2);
	}

	private Compiler compiler = new SimpleCompiler();

	@Override
	public <T extends Protocol> RuntimeAgent<T> compile(T pinstance) {
		return compiler.compile(pinstance);
	}

	@Override
	public ReentrantLock getProtocolInstanceLock() {
		return lock;
	}

}
