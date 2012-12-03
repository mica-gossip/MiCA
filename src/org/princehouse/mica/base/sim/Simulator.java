package org.princehouse.mica.base.sim;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.RuntimeInterface;

/**
 * Single-threaded MiCA emulator.
 * 
 * @author lonnie
 * 
 */
public class Simulator implements RuntimeInterface {

	protected class Event implements Comparable<Event> {
		public long t;

		private boolean cancelled = false;
		
		public void cancel() {
			cancelled = true;
		}
		
		@Override
		public int compareTo(Event e) {
			return Long.valueOf(t).compareTo(Long.valueOf(e.t));
		}

		public boolean isCancelled() {
			return cancelled;
		}
		
		/**
		 * Returns how long the event took, in simulation time units. "now" is
		 * current time when event starts
		 * 
		 * @return
		 */
		public void execute() throws FatalErrorHalt, AbortRound {
		}

		// successCallback should release the lock!
		public void doWithLock(Address a, long timeout,
				SuccessEvent successCallback, TimeoutEvent timeoutCallback) throws FatalErrorHalt, AbortRound {
			if (acquireLock(a)) {
				// we got the lock!
				successCallback.t = t;
				successCallback.timeoutCallback = null;
				successCallback.execute();
			} else {
				addLockWaiter(a, successCallback);
				successCallback.timeoutCallback = timeoutCallback;
				timeoutCallback.successCallback = successCallback;
				timeoutCallback.address = a;
				timeoutCallback.t = t + timeout;
				schedule(timeoutCallback);
			}
		}

		public void scheduleLockRelease(Address a, long rtime) {
			schedule(new LockReleaseEvent(a, rtime));
		}
	}

	protected class LockReleaseEvent extends Event {
		private Address address;
		public LockReleaseEvent(Address address, long t) {
			this.t = t;
			this.address = address;
		}
		@Override
		public void execute() throws FatalErrorHalt, AbortRound {
			releaseLock(address,t);
		}
	}
	
	protected class SuccessEvent extends Event {
		public TimeoutEvent timeoutCallback = null;

		@Override
		public void execute() throws FatalErrorHalt, AbortRound {
			if (timeoutCallback != null) {
				timeoutCallback.cancelled = true;
			}
		}
	}

	protected class TimeoutEvent extends Event {
		public Event successCallback = null;
		public Address address = null;
		public boolean cancelled = false;

		public void execute() {
			if (!cancelled) {
				removeLockWaiter(address, successCallback);
			}
		}
	}

	protected class EventBind extends Event {
		private SimRuntime<? extends Protocol> runtime = null;

		public EventBind(SimRuntime<? extends Protocol> runtime) {
			this.runtime = runtime;
		}

		@Override
		public void execute() throws FatalErrorHalt, AbortRound {
			Address a = runtime.getAddress();
			addressBindings.put(a, runtime);
			acquireLock(a);
			releaseLock(a, t);
		}
	};

	protected class EventInitGossip extends Event {
		private Address src;
		public EventInitGossip(Address src) {
			this.src = src;
		}
		
		@Override
		public void execute() throws FatalErrorHalt, AbortRound {
			SuccessEvent successCallback = new SuccessEvent() {
				@Override
				public void execute() throws FatalErrorHalt, AbortRound {
					// we have the lock.
					// call select, get lock for other
					// TODO
					SimRuntime<?> rt = getRuntime(src);
					assert(rt != null);
					Address dst = rt.select();
					
				}
			};
			
			// failed to acquire lock
			TimeoutEvent timeoutCallback = new TimeoutEvent() {
				public void execute() {
					// FIXME -- log message for init gossip failure - initiator failed to get its own lock
				}
			};
			
			// FIXME hardcoded timeout	
			doWithLock(src, 30000, successCallback, timeoutCallback);
		}
	}
		
	
	
	private boolean acquireLock(Address a) {
		if(!addressBindings.containsKey(a)) {
			return false;
		}
		if (locked.contains(a)) {
			return false;
		} else {
			locked.add(a);
			return true;
		}
	}

	// lock is released at time t
	private void releaseLock(Address a, long t) throws FatalErrorHalt, AbortRound {
		assert (locked.contains(a));
		if(lockWaitQueues.containsKey(a)) {
			List<Event> lockq = lockWaitQueues.get(a);
			if(lockq.size() > 0) {
				Event triggered = lockq.remove(0);
				triggered.t = t;
				triggered.execute();
				return;
			}
		}
		locked.remove(a);
	}

	private LinkedList<Event> eventQueue = new LinkedList<Event>();

	private Map<Address, SimRuntime<?>> addressBindings = Functional
			.map();
	private Set<Address> locked = Functional.set();

	private Map<Address, List<Event>> lockWaitQueues = Functional.map();

	private void addLockWaiter(Address a, Event acquisitionCallback) {
		if (!lockWaitQueues.containsKey(a)) {
			lockWaitQueues.put(a, Functional.list(acquisitionCallback));
		} else {
			List<Event> lockq = lockWaitQueues.get(a);
			if(lockq.size() > 0) {
				// TODO --- is it possible for events with lesser timestamp to be added to the end of queue?  
				// If events are being added out of order, it will be a problem, we'll need to insert at appropriate place
				assert(lockq.get(lockq.size()-1).t <= acquisitionCallback.t);
			}
			lockWaitQueues.get(a).add(acquisitionCallback);
		}
	}

	private void removeLockWaiter(Address a, Event acquisitionCallback) {
		lockWaitQueues.get(a).remove(acquisitionCallback);
	}

	public void schedule(Event e, long t) {
		e.t = t;
		schedule(e);
	}
	
	public void schedule(Event e) {
		assert (e != null);
		Event pred = null; // insertion point
		for (int i = eventQueue.size() - 1; i >= 0; i--) {
			pred = eventQueue.get(i);
			if (pred.compareTo(e) <= 0) {
				eventQueue.add(i, e);
				return;
			}
		}
		eventQueue.add(0, e);
	}

	private boolean running = false;

	@Override
	public void run() {
		// run the simulation
		running = true;
		long time = 0L;
		while (running && eventQueue.size() > 0) {
			Event e = eventQueue.removeFirst();
			assert (e.t >= time);
			time = e.t;
			if(e.isCancelled())
				continue;
			try {
				e.execute();
			} catch (FatalErrorHalt ex) {
				// kill the runtime
			} catch (AbortRound ex) {
				// cancel the round (release locks if applicable)
				// TODO FIXME
				//e.abortRound();
			}
		}
		running = false;
	}

	protected void stopRuntime(SimRuntime<?> rt) {
		// TODO
	}

	protected void startRuntime(SimRuntime<?> rt) {
		// TODO register a new runtime and mark it for execution --- but do not
		// actually run the simulator yet
		if(running) {
			throw new RuntimeException("can't add runtime when simulator running");
		}
		startRuntime(rt,0);
	}
	
	protected void startRuntime(SimRuntime<?> rt, long t) {
		if(running) {
			throw new RuntimeException("can't add runtime when simulator running");
		}
		schedule(new EventBind(rt), t);
	}

	// Simulator is a singleton...
	private static Simulator singleton = null;

	public static Simulator v() {
		if (singleton == null) {
			singleton = new Simulator();
		}
		return singleton;
	}

	
	private SimRuntime<?> getRuntime(Address a) {
		SimRuntime<?> rt = addressBindings.get(a);
		return rt;
	}
	
	protected RuntimeState getRuntimeState(Protocol p) {
		// TODO Auto-generated method stub
		return null;
	}

	public <P extends Protocol> P getReceiver(SimConnection sc) {
		// TODO Auto-generated method stub
		return null;
	}

	public <P extends Protocol> P getSender(SimConnection sc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <P extends Protocol> void addRuntime(Address address, P protocol,
			long randomSeed, int roundLength, int startTime, int lockTimeout) {
		// TODO
		throw new RuntimeException("not implemented yet");
	}

	@Override
	public void scheduleTask(long delay, TimerTask task) {
		throw new RuntimeException("not implemented in sim runtime");
	}

	@Override
	public void stop() {
		running = false;
	}

}
