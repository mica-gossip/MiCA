package org.princehouse.mica.base.simple;


import java.io.IOException;
import java.net.ConnectException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.MalformedViewException;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeAgent;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
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

	private ReentrantLock lock = new ReentrantLock();

	public static int DEFAULT_INTERVAL = 1500; // 1.5 seconds
	private static long LOCK_WAIT_MS = DEFAULT_INTERVAL;

	public static long DEFAULT_RANDOM_SEED = 0L;

	public Address address;

	public SimpleRuntime(Address address) {
		super();
		setAddress(address);
	}
	
	//public SimpleRuntime() { 
	//	this(null);
	//}

	public void setAddress(Address address) {
		if(this.address != null && !this.address.equals(address)) {
			throw new RuntimeException("previous address non-null; cannot change the address of an existing runtime");
		}
		this.address = address;
		runtimeState.setAddress(address);
	}
	/**
	 * Entry point for SimpleRuntime.  Starts a protocol in a new thread. 
	 * Uses SimpleRuntime.DEFAULT_INTERVAL as the gossip interval and 
	 * SimpleRuntime.DEFAULT_RANDOM_SEED as the random seed.
	 * 
	 * @param pinstance Local protocol instance
	 * @param address Local address
	 * @param daemon Launch thread as a daemon
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launch(Runtime<T> rt, final T pinstance,
	 final boolean daemon) {
		return launch(rt, pinstance, daemon, DEFAULT_INTERVAL, DEFAULT_RANDOM_SEED);
	}
	
	/**
	 * Entry point for SimpleRuntime.  Starts a protocol in a new thread. 
	 * 
	 * @param pinstance Local protocol instance
	 * @param address Local address
	 * @param daemon Launch thread as a daemon
	 * @param intervalMS Milliseconds to sleep between each gossip initiation
	 * @param randomSeed Random seed to use for this runtime
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launch(final Runtime<T> rt, final T pinstance,
			 final boolean daemon, final int intervalMS, final long randomSeed) {
		Thread t = new Thread() {
			public void run() {
				try {
					rt.run(pinstance, intervalMS,
							randomSeed);
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
	 * COMPAT MODE --- DO NOT USE
	 * should explicitly create a runtime and call Runtime.setRuntime BEFORE creating protocol instance 
	 * 
	 * @param pinstance
	 * @param address
	 * @return
	 */
	@Deprecated
	public static <T extends Protocol> Runtime<T> launchDaemon(final T pinstance, Address address) {
		Runtime<T> rt = new SimpleRuntime<T>(address);
		return launch(rt, pinstance,true);
	}
	
	/**
	 * Entry point for SimpleRuntime.  Starts a protocol in a new thread. 
	 * (Calls through to launch(), with the daemon flag true)
	 * 
	 * @param rt SimpleRuntime instance -- must be created prior to protocol instance creation
	 * @param pinstance Local protocol instance
	 * @param address Local address
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launchDaemon(Runtime<T> rt, final T pinstance
			) {
		return launch(rt, pinstance,true);
	}

	/**
	 * Entry point for SimpleRuntime.  Starts a protocol in a new thread. 
	 * (Calls through to launch(), with the daemon flag true)
	 * 
	 * @param pinstance Local protocol instance
	 * @param address Local address
	 * @param intervalMS milliseconds to sleep between each gossip initiation
	 * @param randomSeed random seed to be used for this runtime
	 * @return New Runtime instance
	 */
	public static <T extends Protocol> Runtime<T> launchDaemon(SimpleRuntime<T> rt, final T pinstance,
			final Address address, int intervalMS, long randomSeed) {
		return launch(rt, pinstance,true, intervalMS, randomSeed);
	}
	
	
	private P pinstance;

	@Override
	public void acceptConnection(Address recipient, Connection connection)
			throws IOException {
		try {
			if (lock.tryLock(LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {

				setRuntime(this);

				logJson("accept-lock-succeed", recipient);
				((SimpleRuntimeAgent<P>) compile(pinstance)).acceptConnection(
						this, getProtocolInstance(), connection);
				clearRuntime(this);
				lock.unlock();
			} else {
				// failed to acquire lock; timeout

				//if(Runtime.LOGGING_CSV)   Can't do instance-based logging since setRuntime hasn't happened
				//	((BaseProtocol)pinstance).log("accept-lock-fail");

				logJson("accept-lock-fail", recipient);

				System.err
				.printf("%s accept: failed to acquire lock (timeout)\n", this);
				connection.close();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean running = true;

	@Override
	public void run(P pinstance, int intervalMS,
			long randomSeed) throws InterruptedException {

		final Address address = getAddress();
		
		super.run(pinstance, intervalMS, randomSeed);

		// Initialize RuntimeState available to protocols
		runtimeState.setAddress(address);
		runtimeState.setRandom(new Random(randomSeed));

		setProtocolInstance(pinstance);

		try {
			address.bind(this);
		} catch (IOException e1) {
			punt(e1);
		}

		long lastElapsedMS = 0L;

		Random rng = new Random(randomSeed);

		logJson("state-initial",getProtocolInstance().getLogState());
		//logJson("view",getProtocolInstance().getView());
		
		while (running) {
			double rate = getRate(getProtocolInstance());

			logJson("rate",rate);

			int intervalLength = (int) (((double) intervalMS) / rate);
			if(intervalLength <= 0) {
				System.err.printf("%s error: Rate * intervalMS <= 0.  Resetting to default.\n", this);
				intervalLength  = intervalMS;
			}
			Thread.sleep(Math.max(0L, intervalLength - lastElapsedMS));
			if (!running)
				break;

			long startTime = getTimeMS();

			Connection connection = null;
			Address partner = null;

			try {
				if (lock.tryLock(LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {

					RuntimeAgent<P> agent = compile(getProtocolInstance());

					Logging.SelectEvent se = agent.select(this,
							getProtocolInstance(), rng.nextDouble());

					partner = se.selected;
					
					Runtime.debug.printf("%s select %s\n", this, partner);
					
					logJson("select", se);
					//logJson("select", String.format("%s",partner));
					
					try {
						// preUpdate is called even if partner is invalid (null or self address)
						getProtocolInstance().preUpdate(partner);
						logJson("state-pre-update", getProtocolInstance().getLogState());
						//logJson("view",getProtocolInstance().getView());

					} catch(Throwable t) {
						logJson("pre-update-throwable", new Object[]{"preUpdate() threw throwable", t});
					}
					
					if(getAddress().equals(partner)) {
						logJson("failure-self-gossip-attempt");
						continue;
					} else if (partner == null) {
						agent.handleNullSelect(this, getProtocolInstance());
						lock.unlock();
						continue;
					}


					try {
						connection = partner.openConnection();
					} catch(ConnectException ce) {
						agent.handleConnectException(this, pinstance, partner,ce);
                        lock.unlock();
						continue;
					}

					if (!running) {
						lock.unlock();
						break;
				    } 	

					try {
						agent.gossip(this, getProtocolInstance(),
								connection);
					} catch(Throwable t) { 
						// May be a serialization problem!
						logJson("mica-internal-exception", new Object[]{"agent.gossip unexpectedly threw a throwable.  Possible serialization reference cycle",t});		
					}

					try {
						getProtocolInstance().postUpdate();
						logJson("state-post-update", getProtocolInstance().getLogState());
						//logJson("view",getProtocolInstance().getView());

					} catch(Throwable t) {
						logJson("post-update-throwable", new Object[]{"postUpdate() threw throwable", t});
					}

					getRuntimeState().incrementRound();
					
					lock.unlock();
				} else {
					// failed to acquire lock within time limit; gossip again
					// next round
					logJson("lockfail-active");
				}
			} catch (IOException e) {
				lock.unlock();
				this.tolerate(e);
			} catch (SelectException e) {
				lock.unlock();
				this.tolerate(e);
			}
			lastElapsedMS = getTimeMS() - startTime;
		}
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
	public Distribution<Address> getView(
			Protocol protocol) throws SelectException {
		
		Distribution<Address> view = compile(protocol).getView(this, protocol);

		if(view != null && view.isEmpty())
			return null;

		if(view != null && !view.isOne()) {
			throw new MalformedViewException(protocol, view);
		}
				
		return view;
	}

	@Override
	public double getRate(Protocol protocol) {
		return compile(protocol).getRate(this,protocol);
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
