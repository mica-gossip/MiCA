package org.princehouse.mica.base.runtime.implementation;


import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.compiler.impl.SimpleCompiler;
import org.princehouse.mica.base.compiler.impl.SimpleRuntimeAgent;
import org.princehouse.mica.base.compiler.model.Compiler;
import org.princehouse.mica.base.compiler.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.base.runtime.RuntimeState;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.WeakHashSet;


/**
 * Demo runtime implementation
 * 
 */
public class SimpleRuntime<P extends Protocol> extends Runtime<P> implements
AcceptConnectionHandler {

	/**
	 * Entry point for simple runtime basic usage
	 * 
	 * @param pinstance
	 * @return
	 */

	private ReentrantLock lock = new ReentrantLock();

	public static int DEFAULT_INTERVAL = 1500; // 1.5 seconds
	private static long LOCK_WAIT_MS = DEFAULT_INTERVAL;

	public static long DEFAULT_RANDOM_SEED = 0L;

	public Address address;

	public SimpleRuntime(Address address) {
		super();
		this.address = address;
	}

	public static <T extends Protocol> Runtime<T> launch(final T pinstance,
			final Address address) {
		final Runtime<T> rt = new SimpleRuntime<T>(address);
		Thread t = new Thread() {
			public void run() {
				try {
					rt.run(pinstance, address, DEFAULT_INTERVAL,
							DEFAULT_RANDOM_SEED);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
		return rt;
	}

	private P pinstance;

	@Override
	public void acceptConnection(Address recipient, Connection connection)
			throws IOException {
		try {
			if (lock.tryLock(LOCK_WAIT_MS, TimeUnit.MILLISECONDS)) {
				setRuntime(this);
				((SimpleRuntimeAgent<P>) compile(pinstance)).acceptConnection(
						this, getProtocolInstance(), recipient, connection);
				clearRuntime(this);
				lock.unlock();
			} else {
				// failed to acquire lock; timeout
				//((BaseProtocol)pinstance).log("accept-lock-fail");
				System.err
				.printf("%s accept: failed to acquire lock (timeout)", this);
				connection.close();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean running = true;

	@Override
	public void run(P pinstance, Address address, int intervalMS,
			long randomSeed) throws InterruptedException {

		super.run(pinstance, address, intervalMS, randomSeed);

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

		((BaseProtocol) pinstance).logstate();

		while (running) {
			double rate = getFrequency(pinstance);
			((BaseProtocol) pinstance).log("rate,%g",rate);
			int intervalLength = (int) (((double) intervalMS) / rate);
			if(intervalLength <= 0) {
				System.err.printf("%s error: Frequency * intervalMS <= 0.  Resetting to default.\n", this);
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
					partner = compile(pinstance).select(this,
							pinstance, rng.nextDouble());

					Runtime.debug.printf("%s select %s\n", this, partner);

					((BaseProtocol) pinstance).log("select,%s", partner);

					if (partner == null)
						continue;


					connection = partner.openConnection();
					if (!running)
						break;
					compile(pinstance).gossip(this, getProtocolInstance(),
							connection);
					lock.unlock();
				} else {
					// failed to acquire lock within time limit; gossip again
					// next round
					Runtime.debug.printf(
							"%s active lock fail on init gossip [already engaged in gossip?]\n", this);
					((BaseProtocol) pinstance).log("lockfail-active");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			lastElapsedMS = getTimeMS() - startTime;
		}
	}

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

	public void setForeignState(WeakHashSet<Object> foreignObjects,
			RuntimeState foreignState) {
		// only knows about foreign BaseProtocol subclasses for now
		this.foreignObjects = foreignObjects;
		this.foreignState = foreignState;
	}

	public void clearForeignState() {
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
	public Distribution<Address> getSelectDistribution(
			Protocol protocol) {
		Distribution<Address> dist = compile(protocol).getSelectDistribution(this, protocol);
		if(dist == null)
			dist = new Distribution<Address>(); // empty

		return dist;
	}

	@Override
	public double getFrequency(Protocol protocol) {
		return compile(protocol).getFrequency(this,protocol);
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

}