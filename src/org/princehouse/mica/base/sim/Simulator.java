package org.princehouse.mica.base.sim;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

public class Simulator {

	private ReentrantLock simlock = new ReentrantLock();

	protected class ScheduledEvent implements Comparable<ScheduledEvent> {
		public ScheduledEvent(long t, Event e) {
			this.event = e;
			this.t = t;
		}

		public long t;
		public Event event = null;

		@Override
		public int compareTo(ScheduledEvent arg0) {
			return Long.valueOf(t).compareTo(Long.valueOf(arg0.t));
		}

	}

	protected class Event {
		/**
		 * Returns how long the event took, in simulation time units. "now" is
		 * current time when event starts
		 * 
		 * @return
		 */
		public long execute(long now) {
			return 0;
		}
	}

	protected class EventBind extends Event {
		private Runtime<? extends Protocol> runtime = null;

		public EventBind(Runtime<? extends Protocol> runtime) {
			this.runtime = runtime;
		}

		@Override
		public long execute(long now) {
			addressBindings.put(runtime.getAddress(), runtime);
			return 0; // TODO add network timings
		}
	};

	protected class EventOpenChannel extends Event {
		protected Address src;
		protected Address dst;
		protected int timeout;

		public EventOpenChannel(Address src, Address dst) {
			this.src = src;
			this.dst = dst;
		}

		@Override
		public long execute(long now) {
			try {
				simlock.lock();
				// TODO add network wait time
				//blockUntilAvailable(dst, new )
			} finally {
				simlock.unlock();
			}
			return 0; // FIXME 
		}

		private void channelOpened() {
			try {
				simlock.lock();
				// FIXME
				assert (!isBusy(dst));
				busy.add(dst);
			} finally {
				simlock.unlock();
			}
		}
	}

	protected class EventOpenChannelTimeout extends Event {
		private EventOpenChannel openEvent = null;

		public EventOpenChannelTimeout(EventOpenChannel openEvent) {
			this.openEvent = openEvent;
		}
	}

	private boolean isBusy(Address a) {
		try {
			simlock.lock();
			return busy.contains(a);
		} finally {
			simlock.unlock();
		}
	}

	private void setBusy(Address a, boolean b) {
		try {
			simlock.lock();
			if (b) {
				busy.add(a);
			} else if (busy.contains(a)) {
				busy.remove(a);
			}
		} finally {
			simlock.unlock();
		}
	}

	private Queue<Event> eventQueue = new LinkedList<Event>();
	private Map<Address, Runtime<? extends Protocol>> addressBindings = Functional
			.map();
	private Set<Address> busy = Functional.set();

	public void schedule(Event e) {
		simlock.lock();
		eventQueue.add(e);
		simlock.unlock();
	}

	protected void stopRuntime(SimRuntime<?> rt) {
		// TODO
	}

	protected void startRuntime(SimRuntime<?> rt) {
		// TODO register a new runtime and mark it for execution --- but do not
		// actually run the simulator yet
	}

	// Simulator is a singleton...
	private static Simulator singleton = null;

	public static Simulator v() {
		if (singleton == null) {
			singleton = new Simulator();
		}
		return singleton;
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

	public void simulate() {
		// TODO
	}

}
