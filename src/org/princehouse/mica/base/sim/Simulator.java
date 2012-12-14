package org.princehouse.mica.base.sim;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimerTask;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaRuntimeException;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeInterface;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.dummy.DummyAddress;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.reflection.FindReachableObjects;

import fj.F;

/**
 * Single-threaded MiCA emulator.
 * 
 * @author lonnie
 * 
 */
public class Simulator implements RuntimeInterface {

	private long clock = 0; // current clock

	public long getClock() {
		return clock;
	}

	@Override
	public void reset() {
		stop();
		clock = 0;
		eventQueue.clear();
		addressBindings.clear();
		lockHolders.clear();
		lockWaitQueues.clear();
		unlockedQueue.clear();
		running = false;
	}

	
	public void setClock(long clock) {
		this.clock = clock;
	}

	private LinkedList<SimulatorEvent> eventQueue = new LinkedList<SimulatorEvent>();

	private Map<Address, SimRuntime<?>> addressBindings = Functional.map();

	// maps lock_address -> lock_holder_address
	// 
	// lock_holder will never be none (keys will just be removed)
	// yes, we could track this with a set, but this lets us sanity-check the property
	// that only a lock-holder should be able to release a lock
	private Map<Address,Address> lockHolders = new HashMap<Address,Address>();

	private Map<Address, List<SimulatorEvent>> lockWaitQueues = Functional
			.map();

	private List<Address> unlockedQueue = Functional.list();

	public void bind(Address address, SimRuntime<?> rt, int starttime) {
		addressBindings.put(address, rt);
		markUnlocked(address);
		rt.start();
		new SimRound(address, this, starttime);
	}
	
	public void unbind(SimRuntime<?> rt) {
		addressBindings.remove(rt.getAddress());
		markUnlocked(rt.getAddress());
	}
	
	/**
	 * Returns false if lock failed, true if successful otherwise Fails if lock
	 * already held by someone else, or if the address is not bound
	 * 
	 * @param lock
	 * @return
	 */
	protected boolean lock(Address lock, Address requestor) {
		if (!addressBindings.containsKey(lock)) {
			return false;
		}
		if (lockHolders.containsKey(lock)) {
			return false;
		} else {
			lockHolders.put(lock, requestor);
			return true;
		}
	}

	// lock is being released at time t
	protected void unlock(Address lock, Address requestor) {
		if (lockHolders.containsKey(lock)) {
			Address holder = lockHolders.get(lock);
			if(!requestor.equals(holder)) {
				throw new RuntimeException("tried to unlock an address locked by someone else");
			}
			lockHolders.remove(lock);
			markUnlocked(lock);
		} else {
			throw new RuntimeException("tried to unlock an already-unlocked address");
		}
	}

	protected void markUnlocked(Address a) {
		unlockedQueue.add(a);
	}

	/*
	 * if(lockWaitQueues.containsKey(a)) { List<SimulatorEvent> lockq =
	 * lockWaitQueues.get(a); while(lockq.size() > 0) { SimulatorEvent triggered
	 * = lockq.remove(0); if(triggered.isCancelled()) continue;
	 * triggered.execute(this); return; } } locked.remove(a); }
	 */

	protected void addLockWaiter(Address a, SimulatorEvent acquisitionCallback) {
		if (!lockWaitQueues.containsKey(a)) {
			lockWaitQueues.put(a, Functional.<SimulatorEvent> list());
		}
		acquisitionCallback.t = getClock();
		schedule(acquisitionCallback, lockWaitQueues.get(a));
	}

	public void schedule(SimulatorEvent e) {
		schedule(e, eventQueue);
	}

	public void scheduleRelative(SimulatorEvent e, long offset) {
		assert(offset >= 0);
		e.t = getClock() + offset;
		schedule(e, eventQueue);
	}

	public SimulatorEvent pollEventQueue(List<SimulatorEvent> queue) {
		if (queue == null) {
			return null;
		}

		while (queue.size() > 0) {
			SimulatorEvent e = queue.remove(0);
			if (e.isCancelled()) {
//				SimRuntime.debug.printf("   (cancelled)@%d   %s\n", e.t,
//						e.toString());
				continue;
			} else {
				return e;
			}
		}
		return null;
	}

	public void schedule(SimulatorEvent e, List<SimulatorEvent> queue) {
		assert (e != null);

		assert(e.t >= getClock());
		
		//if (queue == eventQueue) {
		//	SimRuntime.debug.printf("    schedule @ %d: %s\n", e.t, e.toString());
		//}
		boolean added = false;

		for (ListIterator<SimulatorEvent> it = queue.listIterator(queue.size()); it
				.hasPrevious();) {
			if (it.previous().t <= e.t) {
				it.next();
				it.add(e);
				added = true;
				break;
			}
		}

		// sanity check
		//assert(queueIsCorrectlySorted(queue));
		
		if (!added) {
			// all events in queue have timestamps greater than e.t
			queue.add(0, e);
		}

	}

	@SuppressWarnings("unused")
	private boolean queueIsCorrectlySorted(List<SimulatorEvent> queue) {
		// sanity check --- O(n), use sparingly
		long t = 0;
		for(SimulatorEvent e : queue) {
			if(e.t < t) return false;
			t = e.t;
		}
		return true;
	}
	
	private boolean running = false;

	private SimulatorEvent getNextEvent() {
		while (unlockedQueue.size() > 0) {
			Address a = unlockedQueue.remove(0);
			SimulatorEvent callback = pollEventQueue(lockWaitQueues.get(a));
			if (callback != null) {
				callback.t = getClock();
				return callback;
			}
		}
		return pollEventQueue(eventQueue);
	}

	@Override
	public void run() {
		// run the simulation
		running = true;
		setClock(0L);
		
		StopWatch simtimer = new StopWatch();
		simtimer.reset();
		
		long round = 0;
		int roundSize = MiCA.getOptions().roundLength;
		
		// write options message to the first runtime
		Runtime<?> arbitraryRuntime = addressBindings.values().iterator().next();
		arbitraryRuntime.logJson(LogFlag.init, "mica-options", MiCA.getOptions());
		
		while (running) {
			long curRound = (getClock() / roundSize) + 1;
			if(curRound != round) {
				String stopsfx;
				if(MiCA.getOptions().stopAfter > 0) {
					stopsfx = String.format(" of %s", MiCA.getOptions().stopAfter);
				} else {
					stopsfx = "";
				}
				
				SimRuntime.debug.printf("(%s) round %d%s\n", MiCA.getOptions().expname, curRound, stopsfx);
				round = curRound;
			}
			SimulatorEvent e = getNextEvent();
			if (e == null) {
				break;
			}
			
			SimRuntime<?> rt = getRuntime(e.getSrc());
			long clock = getClock();
			
			//String msg = String.format("@%d -> %d execute %s", clock, e.t, e.toString());
			//rt.logJson("debug-event", msg);
			//SimRuntime.debug.println(msg);

			assert (e.t >= clock);
			setClock(e.t);

			try {		
				e.execute(this);
			} catch (AbortRound ex) {
				if(e instanceof SimRound.RoundEvent){
					e.abortRound(this);
				}
			} catch (FatalErrorHalt ex) {
				if(e instanceof SimRound.RoundEvent){
					e.abortRound(this);
				}
				Address src = e.getSrc();
				if(src != null) {
					killRuntime(getRuntime(src));
				}
			} catch (MicaRuntimeException ex) {
				// dead code
				ex.printStackTrace();
			}
		}
		running = false;
		
		double sfac = ((double)getClock())/((double)simtimer.elapsed()+1);
		SimRuntime.debug.printf("Simulator stopped @%d; speed-up factor of %f\n", getClock(), sfac);
	}

	protected void stopRuntime(SimRuntime<?> rt) {
		unbind(rt);
	}


	// Simulator is a singleton...
	private static Simulator singleton = null;

	public static Simulator v() {
		if (singleton == null) {
			singleton = new Simulator();
		}
		return singleton;
	}

	protected SimRuntime<?> getRuntime(Address a) {
		SimRuntime<?> rt = addressBindings.get(a);
		return rt;
	}

	protected RuntimeState getRuntimeState(Protocol p) {
		return getRuntime(p).getRuntimeState();
	}

	public <P extends Protocol> P getReceiver(SimConnection sc) {
		throw new UnsupportedOperationException();
	}

	public void killRuntime(SimRuntime<?> rt) {
		rt.stop();
	}

	public <P extends Protocol> P getSender(SimConnection sc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <P extends Protocol> Runtime<?> addRuntime(Address address, P protocol,
			long randomSeed, int roundLength, int startTime, int lockTimeout) {
		SimRuntime<P> rt = new SimRuntime<P>(address);
		rt.setProtocolInstance(protocol);
		rt.setRandomSeed(randomSeed);
		rt.setRoundLength(roundLength);
		rt.setLockWaitTimeout(lockTimeout);
		bind(address, rt, startTime);
		return rt;
	}

	@Override
	public void scheduleTask(long delay, TimerTask task) {
		this.scheduleRelative(new TimerEvent(null, task), delay);
	}

	@Override
	public void stop() {
		running = false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Protocol> Runtime<T> getRuntime(Protocol p) {
		if(runtimeSingleNode != null) {
			return (Runtime<T>) runtimeSingleNode;
		}
		
		Runtime<T> rt = (Runtime<T>) protocolRuntimeContext.get(p);
		if (rt == null) {
			throw new RuntimeException(String.format(
					"runtime %x is null for %s", p.hashCode(), p.getClass()
							.getName()));
		}
		return rt;
	}

	// Maps protocol -> runtime
	// for all reachable protocol objects
	private Map<Protocol, SimRuntime<?>> protocolRuntimeContext = Functional
			.map();


	private SimRuntime<?> runtimeSingleNode = null;
	
	public void setRuntimeSingleNode(SimRuntime<?> rt) {
		assert(runtimeSingleNode == null);
		runtimeSingleNode = rt;
	}
	
	public void clearRuntimeSingleNode() {
		runtimeSingleNode = null;
	}
	
	@Override
	public <T extends Protocol> void setRuntime(Runtime<T> rt) {
		assert(runtimeSingleNode == null);
		FindReachableObjects<Protocol> reachableProtocolFinder = new FindReachableObjects<Protocol>() {
			@Override
			public boolean match(Object obj) {
				return (obj instanceof Protocol);
			}
		};

		for (Protocol p : reachableProtocolFinder
				.find(rt.getProtocolInstance())) {

			SimRuntime<?> previousEntry = protocolRuntimeContext.get(p);

			if (previousEntry != null && previousEntry != rt) {
				throw new RuntimeException(
						String.format(
								"protocol instance has two conflicting runtime contexts: rt-%x, rt-%x  -->  %x (%s)\n",
								previousEntry.hashCode(), rt.hashCode(),
								p.hashCode(), p.getClass().getName()));
			}
			protocolRuntimeContext.put(p, (SimRuntime<?>) rt);
		}
	}

	/**
	 * Default node name is options.expname + i
	 */
	@Override
	public F<Integer, Address> getAddressFunc() {
		return new F<Integer, Address>() {
			@Override
			public Address f(Integer i) {
				return new DummyAddress(String.format("%s%d",MiCA.getOptions().expname,i));
			}
		};
	}

}
