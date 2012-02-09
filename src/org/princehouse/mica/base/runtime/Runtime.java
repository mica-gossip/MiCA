package org.princehouse.mica.base.runtime;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.compiler.model.RuntimeAgent;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;




public abstract class Runtime<P extends Protocol> {
	
	
	public static final PrintStream debug = System.err;


	private static final ReentrantLock loglock = new ReentrantLock();
	private static long startingTimestamp = 0;
	private static int logEventCounter = 0;
	
	private static int uidCounter = 0;
	private static final ReentrantLock uidlock = new ReentrantLock();

	public static int getNewUID() {
		uidlock.lock();
		int x = uidCounter++;
		uidlock.unlock();
		return x;
	}
	
	public static void log(String msg) {
		loglock.lock();
	
		File logfile = new File("log.csv");
			
	    long timestamp = new Date().getTime();
	    if(startingTimestamp == 0) {
	    	startingTimestamp = timestamp;
	    	if(logfile.exists()) {
	    		logfile.delete();
	    	}
	    }
	    timestamp -= startingTimestamp;

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(logfile, logfile.exists());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			loglock.unlock();
			return;
		}
		PrintStream out = new PrintStream(fos);
		out.printf("%d,%d,",logEventCounter++,timestamp);
		out.println(msg);
		try {
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loglock.unlock();
	}
	
	
	/**
	 * Start the runtime
	 * @param <T>
	 * 
	 * @param protocol
	 * @param a
	 * @param intervalMS
	 * @throws InterruptedException
	 */
	
	public abstract <T extends Protocol> RuntimeAgent<T> compile(T pinstance);
	
	
	public void run(P pinstance, Address a, int intervalMS, long randomSeed) throws InterruptedException {
		setRuntime(this);
	};
	
	public abstract P getProtocolInstance();
	
	public abstract void setProtocolInstance(P pinstance);
	
	public abstract void stop();
	
	public abstract Address getAddress();
	
	public <T> T punt(Exception e) {
		throw new RuntimeException(e); 
	}

	public <T> T fatal(Exception e) {
		stop();
		System.err.printf("Fatal exception happened in runtime %s\n",this);
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
	
	private Random random = new Random();
	
	public Random getRandom() {
		return random;
	}
	
	private static ThreadLocal<Runtime<?>> runtimeSingleton = new ThreadLocal<Runtime<?>>();
	
	public static void setRuntime(Runtime<?> rt) {
		//System.err.printf("[set %s for thread %d]\n", rt, Thread.currentThread().getId());
		if(runtimeSingleton.get() != null && rt != null) {
			throw new RuntimeException("attempt to set two runtimes in one thread");
		}
		runtimeSingleton.set(rt);
	}
	
	public static Runtime<?> getRuntime() {
		Runtime<?> rt = runtimeSingleton.get();
		if(rt == null)
			throw new RuntimeException(String.format("Failed attempt to get null runtime for thread %d", Thread.currentThread().getId()));
		return rt;
	}
	
	public static void clearRuntime(Runtime<?> rt) {
		Runtime<?> current = runtimeSingleton.get();
		if(current != null && !current.equals(rt)) { 
			throw new RuntimeException("attempt to replace active runtime");
		}
	
		setRuntime(null);
	}
	
	public abstract RuntimeState getRuntimeState(Protocol p);
	
	// Called by agents.  Protocols should not use directly
	public abstract RuntimeState getRuntimeState();

	public String toString() {
		return String.format("<Runtime %d>", hashCode());
	}
	
	/**
	 * NOTE: must never return null.  return an empty distribution instead.
	 * @param p
	 * @return
	 */
	public abstract Distribution<Address> getSelectDistribution(Protocol p);

	public abstract void executeUpdate(Protocol p1, Protocol p2);

	public abstract double getFrequency(Protocol protocol);

	public long getTime() {
	    return new Date().getTime();
	}
}
