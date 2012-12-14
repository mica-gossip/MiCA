package org.princehouse.mica.base.simple;

import static org.princehouse.mica.base.RuntimeErrorCondition.ACTIVE_GOSSIP_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.BIND_ADDRESS_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.INITIATOR_LOCK_TIMEOUT;
import static org.princehouse.mica.base.RuntimeErrorCondition.NULL_SELECT;
import static org.princehouse.mica.base.RuntimeErrorCondition.OPEN_CONNECTION_FAIL;
import static org.princehouse.mica.base.RuntimeErrorCondition.POSTUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.PREUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.SELF_GOSSIP;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.util.Logging.SelectEvent;

/**
 * Basic Runtime implementation.
 * 
 * Nothing fancy: It just serializes and exchanges complete node state.
 * 
 */
public class SimpleRuntime extends Runtime implements AcceptConnectionHandler {

	public static final boolean DEBUG_NETWORKING = false;

	private ReentrantLock lock = new ReentrantLock();

	// public static int DEFAULT_INTERVAL = 1500; // 1.5 seconds
	// private static long LOCK_WAIT_MS = DEFAULT_INTERVAL;

	// public static long DEFAULT_RANDOM_SEED = 0L;

	public SimpleRuntime(Address address) {
		super();
		setAddress(address);
	}

	@Override
	public void setAddress(Address address) {
		Address current = getAddress();
		if (current != null && !current.equals(address)) {
			throw new RuntimeException(
					"previous address non-null; cannot change the address of an existing runtime");
		}
		super.setAddress(address);
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
	public static Runtime launch(final Runtime rt, final Protocol pinstance,
			final boolean daemon, final int intervalMS, final long randomSeed,
			int lockWaitTimeoutMS) {
		rt.setProtocolInstance(pinstance);
		rt.setInterval(intervalMS);
		rt.setLockWaitTimeout(lockWaitTimeoutMS);
		rt.setRandom(new Random(randomSeed));
		((SimpleRuntime) rt).launchThread(daemon);
		return rt;
	}

	public void launchThread(final boolean daemon) {
		final SimpleRuntime rt = this;

		Thread t = new Thread() {
			public void run() {
				try {
					rt.run();
				} catch (InterruptedException e) {
					rt.stop();
				}
			}
		};
		t.setDaemon(daemon);
		t.start();
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
	public static Runtime launchDaemon(SimpleRuntime rt,
			final Protocol pinstance, final Address address, int intervalMS,
			long randomSeed, int lockWaitTimeoutMS) {
		return launch(rt, pinstance, true, intervalMS, randomSeed,
				lockWaitTimeoutMS);
	}

	private Protocol pinstance;

	@Override
	public void acceptConnection(Address recipient, Connection connection)
			throws IOException, FatalErrorHalt, AbortRound {
		try {
			if (lock.tryLock(getLockWaitTimeout(), TimeUnit.MILLISECONDS)) {
				if (!running) {
					logJson(LogFlag.error, "mica-error-internal",
							"acceptConnection called on a stopped runtime");
					connection.close();
					return;
				}
				((SimpleRuntimeAgent) compile(pinstance)).acceptConnection(
						this, getProtocolInstance(), connection);
				lock.unlock();
			} else {
				if (!running) {
					logJson(LogFlag.error, "mica-error-internal",
							"acceptConnection called on a stopped runtime + lock failed");
					connection.close();
					return;
				}
				// failed to acquire lock; timeout
				logJson(LogFlag.error, "mica-error-accept-connection"); // sim-ok
				System.err.printf(
						"%s accept: failed to acquire lock (timeout)\n", this);
				connection.close();
			}
		} catch (InterruptedException e) {

			handleError(RuntimeErrorCondition.INTERRUPTED, e);

		}
	}

	private boolean running = true;

	@Override
	public void run() throws InterruptedException {

		final Address address = getAddress();
		super.run();
		
		MiCA.getRuntimeInterface().getRuntimeContextManager().setNativeRuntime(this);
		logState("initial"); // sim-ok
		MiCA.getRuntimeInterface().getRuntimeContextManager().clear();


		try {
			address.bind(this);
		} catch (IOException e1) {
			try {
				handleError(BIND_ADDRESS_EXCEPTION, e1);
			} catch (FatalErrorHalt e) {
				return;
			} catch (AbortRound e) {
				return; // same as fatal in this case
			}
		}

		long lastElapsedMS = 0L;

		double rate = 1.0;

		MiCA.getRuntimeInterface().getRuntimeContextManager()
				.setNativeRuntime(this);
		try {
			rate = getProtocolInstance().getRate();
		} catch (Throwable t) {
			try {
				handleError(RuntimeErrorCondition.RATE_EXCEPTION, t);
			} catch (FatalErrorHalt e) {
				this.stop();
				return;
			} catch (AbortRound e) {
				// ignore
			}
		} finally {
			MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
		}

		int intervalMS = getInterval();

		try {
			while (running) {

				Connection connection = null;
				long startTime = getTimeMS();
				Address partner = null;

				try {
					MiCA.getRuntimeInterface().getRuntimeContextManager()
							.setNativeRuntime(this);
					logJson(LogFlag.rate, "mica-rate", rate); // sim-ok
					MiCA.getRuntimeInterface().getRuntimeContextManager()
							.clear();

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

					if (lock.tryLock(getLockWaitTimeout(),
							TimeUnit.MILLISECONDS)) {

						if (!running) {
							// recv thread may have shutdown while it held
							// the lock.
							// now that we have it, test for this
							lock.unlock();
							break;
						}

						RuntimeAgent agent = compile(getProtocolInstance());

						SelectEvent se = null;

						Protocol p = getProtocolInstance();
						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						try {
							se = new SelectEvent();
							se.selected = p.getView().sample(
									p.getRuntimeState().getRandom());
							if (se.selected.equals(p.getAddress())) {
								se.selected = null;
							}
						} catch (Throwable e) {
							handleError(RuntimeErrorCondition.SELECT_EXCEPTION,
									e);
						} finally {
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();
						}

						partner = se.selected;

						logJson(LogFlag.select, "mica-select", se); // sim-ok

						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						try {
							// preUpdate is called even if partner is
							// invalid
							// (null or self address)
							getProtocolInstance().preUpdate(partner);
						} catch (Throwable t) {
							handleError(PREUDPATE_EXCEPTION, t);
						} finally {
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();
						}
						logState("preupdate"); // sim-ok

						if (getAddress().equals(partner)) {
							handleError(SELF_GOSSIP, null);
						} else if (partner == null) {
							handleError(NULL_SELECT, null);
						}

						try {
							connection = partner.openConnection();
						} catch (ConnectException ce) {
							handleError(OPEN_CONNECTION_FAIL, ce);
						} catch (IOException io) {
							handleError(OPEN_CONNECTION_FAIL, io);
						}

						try {
							// no context-setting needed here; the work is done
							// by the receiver
							agent.gossip(this, getProtocolInstance(),
									connection);
						} catch (AbortRound ar) {
							throw ar;
						} catch (FatalErrorHalt feh) {
							throw feh;
						} catch (Throwable t) {
							// May be a serialization problem
							handleError(ACTIVE_GOSSIP_EXCEPTION, t);
						}

						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						logState("gossip-initiator"); // sim-ok
						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.clear();

						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						try {
							getProtocolInstance().postUpdate();
							logState("postupdate"); // sim-ok
						} catch (Throwable t) {
							handleError(POSTUDPATE_EXCEPTION, t);
						} finally {
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();
						}

						getRuntimeState().incrementRound();

						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						try {
							rate = getProtocolInstance().getRate();
						} catch (Throwable t) {
							try {
								handleError(
										RuntimeErrorCondition.RATE_EXCEPTION, t);
							} catch (FatalErrorHalt e) {
								this.stop();
								return;
							} catch (AbortRound e) {
								// ignore
							}
						} finally {
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();
						}
						
						lock.unlock();
					} else {
						// failed to acquire lock within time limit; gossip
						handleError(INITIATOR_LOCK_TIMEOUT, null);
					}
				} catch (AbortRound ar) {
					// close connection, if applicable
					if (connection != null) {
						try {
							connection.close();
						} catch (IOException e) {
						}
					}
					try {
						lock.unlock(); // try to release lock
					} catch (IllegalMonitorStateException ie) {
					}
				}
				lastElapsedMS = getTimeMS() - startTime;
				double sec = ((double) lastElapsedMS) / 1000.0;
				Runtime.debug.printf("%s -> %s, elapsed time %g s\n", this,
						partner, sec);
			}
		} catch (FatalErrorHalt e) {
			stop();
			// fatalErrorHalt should have already shut down everything
		} // end while(running) loop
	}

	/*
	 * @Override protected void fatalErrorHalt(RuntimeErrorCondition condition)
	 * throws FatalErrorHalt { stop(); // passively signal that it's time to
	 * shut down try { getAddress().unbind();// try to unbind the listener }
	 * catch (NotBoundException e1) { } try { lock.unlock(); } catch
	 * (IllegalMonitorStateException e) { } throw new FatalErrorHalt(); }
	 */

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
	public Protocol getProtocolInstance() {
		return pinstance;
	}

	@Override
	public void setProtocolInstance(Protocol pinstance) {
		this.pinstance = pinstance;
	}

	private Compiler compiler = new SimpleCompiler();

	@Override
	public RuntimeAgent compile(Protocol pinstance) {
		return compiler.compile(pinstance);
	}

	@Override
	public ReentrantLock getProtocolInstanceLock() {
		return lock;
	}

	@Override
	public void start() {
		launchThread(true); // launch in a new thread
	}

	@Deprecated
	public static Runtime launchDaemon(Protocol node, Address a) {
		throw new RuntimeException("deprecated");
	}

	@Deprecated
	public static Runtime launchDaemon(Runtime rt, Protocol node) {
		throw new RuntimeException("deprecated");
	}

}
