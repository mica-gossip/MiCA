package org.princehouse.mica.base.model;

import static org.princehouse.mica.base.RuntimeErrorResponse.ABORT_ROUND;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.RuntimeErrorResponse;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.ClassUtils;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.Logging;
import org.princehouse.mica.util.Randomness;

import com.google.gson.Gson;

/**
 * The Runtime instance represents the local node in the gossip network. It runs
 * the local protocol instance with the help of a RuntimeAgent, generated by a
 * Compiler instance.
 * 
 * To launch your own protocol, see the SimpleRuntime.launch method.
 * 
 * @author lonnie
 * 
 * @param <P>
 */
public abstract class Runtime<P extends Protocol> {

	public Runtime() {
	}

	/**
	 * Get a lock that can be used to suspend incoming or outgoing gossip
	 * WARNING: Failure to release this lock will effectively cause node
	 * failure.
	 * 
	 * @return
	 */
	public abstract ReentrantLock getProtocolInstanceLock();

	// Enable new JSON logs
	public static boolean LOGGING_JSON = true;

	/**
	 * Universal debugging printstream. System.err by default; adjust as
	 * necessary.
	 */
	public static PrintStream debug = System.out;

	private static int uidCounter = 0;
	private static final ReentrantLock uidlock = new ReentrantLock();

	private ReentrantLock runtimeLoglock = new ReentrantLock();

	/**
	 * Runtime includes a local unique id generator. (IDs are only unique
	 * locally)
	 * 
	 * @return
	 */
	public static int getNewUID() {
		uidlock.lock();
		int x = uidCounter++;
		uidlock.unlock();
		return x;
	}

	// json log file for this runtime instance
	private File logfile = null;

	// initial value; can be changed with command line options
	private File logDirectory = new File("mica_log");

	public File getLogFile() {
		if (logfile == null) {
			if (!logDirectory.exists()) {
				logDirectory.mkdirs();
			}
			String addr = getAddress().toString();
			addr = addr.replace("/", "_");
			logfile = new File(logDirectory, String.format("%s.log", addr));
		}
		return logfile;
	}

	public void setLogFile(File logfile) {
		this.logfile = logfile;
	}

	public void setLogDirectory(File logDirectory, boolean create) {
		if (create && !logDirectory.exists()) {
			logDirectory.mkdirs();
		}

		if (!logDirectory.exists()) {
			throw new RuntimeException(String.format(
					"Log directory %s does not exist", logDirectory));
		}
		this.logDirectory = logDirectory;
	}

	// private long runtimeStartingTimestamp = 0;

	public long getRuntimeClockMS() {
		// return (new Date().getTime()) - runtimeStartingTimestamp;
		return (new Date().getTime());
	}

	
	public long getRuntimeClock() {
		return getRuntimeClockMS();
	}

	public static class JsonLogEvent {
		public long timestamp;
		public String address;
		public String event_type;
		public Object data;

		public JsonLogEvent(long timestamp, String address, String type,
				Object event) {
			this.timestamp = timestamp;
			this.address = address;
			this.event_type = type;
			this.data = event;
		}
	}

	public void logJson(final String eventType) {
		logJson(eventType, null);
	}

	public void logJson(final String eventType, final Object theEvent) {
		logJson(getAddress(), eventType, theEvent);
	}

	public void logJson(final Address origin, final String eventType,
			final Object theEvent) {

		if (!Runtime.LOGGING_JSON)
			return;

		runtimeLoglock.lock();

		File logfile = getLogFile();

		/**
		 * if(runtimeStartingTimestamp == 0) { runtimeStartingTimestamp = new
		 * Date().getTime(); if(logfile.exists()) { logfile.delete(); } }
		 **/
		// old logfile now deleted at startup

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(logfile, logfile.exists());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			runtimeLoglock.unlock();
			return;
		}

		PrintStream out = new PrintStream(fos);
		JsonLogEvent logobj = new JsonLogEvent(getRuntimeClock(),
				origin.toString(), eventType, theEvent);

		Gson gson = Logging.getGson();

		try {
			String msg = gson.toJson(logobj);
			out.println(msg);
		} catch (StackOverflowError e) {
			Object payload = logobj.data;
			System.err.println(String.format(
					"fatal error: possible reference cycle with %s", payload
							.getClass().getCanonicalName()));
			ClassUtils.debug = true;
			boolean found = ClassUtils.findReferenceCycles(payload);
			if (!found) {
				System.err
						.println("   weirdness: findReferenceCycles returned False (Runtime.java)");
			}
			// Treat this as a fatal error
			System.exit(-1);

		} catch (UnsupportedOperationException f) {
			logJson(origin, "mica-error-internal", f);
			tolerate(f);
		}

		try {
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		runtimeLoglock.unlock();
	}

	public abstract <T extends Protocol> RuntimeAgent<T> compile(T pinstance);

	/**
	 * Start the runtime. If you override this, be sure to call the inherited
	 * run().
	 * 
	 * @param <T>
	 * 
	 * @param protocol
	 *            Local top-level protocol instance
	 * 
	 * @param intervalMS
	 *            Round length, in milliseconds
	 * @param randomSeed
	 *            Local random seed
	 * @throws InterruptedException
	 */
	public void run() throws InterruptedException {
		// clear old log
		File logfile = this.getLogFile();
		if (logfile.exists()) {
			logfile.delete();
		}

		int intervalMS = getInterval();
		long randomSeed = Randomness.getSeed(getRandom());

		logJson("mica-runtime-init", Functional.<String, Object> mapFromPairs(
				"round_ms", intervalMS, "random_seed", randomSeed));

		setRuntime(this);
	};

	/**
	 * Get the local top-level protocol instance
	 * 
	 * @return Local top-level protocol instance
	 */
	public abstract P getProtocolInstance();

	/**
	 * Set local top-level protocol instance
	 * 
	 * @param pinstance
	 */
	public abstract void setProtocolInstance(P pinstance);

	/**
	 * Stop the local runtime
	 */
	public abstract void stop();

	/**
	 * 
	 * @return
	 */
	public Address getAddress() {
		return getRuntimeState().getAddress();
	}

	public void setAddress(Address address) {
		getRuntimeState().setAddress(address);
	}

	public <T> T punt(Exception e) {
		throw new RuntimeException(e);
	}

	public <T> T fatal(Exception e) {
		stop();
		System.err.printf("Fatal exception happened in runtime %s\n", this);
		e.printStackTrace();
		System.exit(1);
		return null;
	}

	public void tolerate(Exception e) {
		// ignore exception, but print diagnostic info
		debug.printf("[%s Suppressed exception: %s]\n", getAddress(), e);
		e.printStackTrace(debug);
	}

	public void handleUpdateException(Exception e) {
		debug.printf("[%s update execution exception: %s]\n", getAddress(), e);
		e.printStackTrace(debug);
	}

	public void handleSelectException(Exception e) {
		debug.printf("[%s select execution exception: %s]\n", getAddress(), e);
		e.printStackTrace(debug);
	}

	private static ThreadLocal<Runtime<?>> runtimeSingleton = new ThreadLocal<Runtime<?>>();

	public static void setRuntime(Runtime<?> rt) {
		// System.err.printf("[set %s for thread %d]\n", rt,
		// Thread.currentThread().getId());
		if (runtimeSingleton.get() != null && rt != null
				&& !runtimeSingleton.equals(rt)) {
			throw new RuntimeException(
					String.format(
							"Attempt to set two runtimes in one thread; existing runtime is %s, new runtime is %s",
							runtimeSingleton.get(), rt));
		}
		runtimeSingleton.set(rt);
	}

	public static Runtime<?> getRuntime() {
		Runtime<?> rt = runtimeSingleton.get();
		if (rt == null)
			throw new RuntimeException(String.format(
					"Failed attempt to get null runtime for thread %d", Thread
							.currentThread().getId()));
		return rt;
	}

	public static void clearRuntime(Runtime<?> rt) {
		Runtime<?> current = runtimeSingleton.get();
		if (current != null && !current.equals(rt)) {
			throw new RuntimeException("attempt to replace active runtime");
		}
		setRuntime(null);
	}

	
	public abstract RuntimeState getRuntimeState(Protocol p);

	private RuntimeState runtimeState = new RuntimeState();

	// Called by agents. Protocols should not use directly
	public RuntimeState getRuntimeState() {
		return runtimeState;
	}
	
	public void setRuntimeState(RuntimeState rts) {
		runtimeState = rts;
	}
	

	public String toString() {
		return String.format("<Runtime %d>", hashCode());
	}

	/**
	 * Returns null if view is an empty distribution
	 * 
	 * Throws MalformedViewException if view has non-one, non-empty magnitude
	 * 
	 * @param p
	 * @return
	 * @throws SelectException
	 */
	public abstract Distribution<Address> getView(Protocol p)
			throws SelectException;

	/**
	 * Run the gossip update on two local objects.
	 * 
	 * Not used by the runtime gossip mechanism, but useful for making composite
	 * protocols, i.e., when a master protocol wants to run the update function
	 * of a subprotocol.
	 * 
	 * @param p1
	 * @param p2
	 */
	public abstract void executeUpdate(Protocol p1, Protocol p2);

	/**
	 * Get the rate for a protocol
	 * 
	 * @param protocol
	 * @return
	 */
	public abstract double getRate(Protocol protocol);

	public long getTime() {
		return new Date().getTime();
	}

	public void handleError(RuntimeErrorCondition condition, Object payload)
			throws FatalErrorHalt, AbortRound {
		logJson("mica-error-internal", payload);
		handleError(condition);
	}

	public void handleError(RuntimeErrorCondition condition, String msg,
			Object payload) throws FatalErrorHalt, AbortRound {
		logJson("mica-error-internal", new Object[] { msg, payload });
		handleError(condition);
	}

	public void handleError(RuntimeErrorCondition condition)
			throws FatalErrorHalt, AbortRound {
		RuntimeErrorResponse policy = getErrorPolicy(condition);
		logJson("mica-error-handler",
				String.format("%s -> %s", condition, policy));
		switch (policy) {
		case FATAL_ERROR_HALT:
			fatalErrorHalt(condition);
			break;
		case IGNORE:
			return; // do nothing at all!
		case ABORT_ROUND:
			tolerateError();
			break;
		default:
			throw new RuntimeException(
					"unhandled error response shouldn't happen");
		}
	}

	protected abstract void tolerateError() throws AbortRound;

	protected abstract void fatalErrorHalt(RuntimeErrorCondition condition)
			throws FatalErrorHalt;

	public RuntimeErrorResponse getErrorPolicy(RuntimeErrorCondition condition) {
		switch (condition) {
		case NULL_SELECT:
			return ABORT_ROUND;
		case OPEN_CONNECTION_FAIL:
			return ABORT_ROUND;
		case INITIATOR_LOCK_TIMEOUT:
			return ABORT_ROUND;
		case GOSSIP_IO_ERROR:
			return ABORT_ROUND;
		default:
			return RuntimeErrorResponse.FATAL_ERROR_HALT;
		}
	}

	public void logState(String label) {
		logJson("mica-state-" + label, getProtocolInstance().getLogState());
	}

	public void setRandomSeed(Long seed) {
		getRuntimeState().setRandom(new Random(seed));
	}

	public void setRoundLength(int roundLength) {
		getRuntimeState().setIntervalMS(roundLength);
	}
	
	/**
	 * Lock wait timeout
	 * @param lockWaitTimeout
	 */
	public void setLockWaitTimeout(int lockWaitTimeout) {
		getRuntimeState().setLockWaitTimeoutMS(lockWaitTimeout);
	}
	
	public int getLockWaitTimeout() {
		return getRuntimeState().getLockWaitTimeoutMS();
	}

	public Random getRandom() {
		return getRuntimeState().getRandom();
	}

	public void setRandom(Random r) {
		getRuntimeState().setRandom(r);
	}

	public int getInterval() {
		return getRuntimeState().getIntervalMS();
	}

	public void setInterval(int intervalMS) {
		getRuntimeState().setIntervalMS(intervalMS);
	}

	public abstract void start();

}
