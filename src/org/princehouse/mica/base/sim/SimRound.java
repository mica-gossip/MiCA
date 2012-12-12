package org.princehouse.mica.base.sim;

import static org.princehouse.mica.base.RuntimeErrorCondition.INITIATOR_LOCK_TIMEOUT;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.MicaRuntimeException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Logging.SelectEvent;

public class SimRound {

	private Address src;
	private Address dst = null;
	private Simulator sim;
	private SimRound round;
	private boolean cancelled = false;

	private boolean haveLockSrc = false;
	private boolean haveLockDst = false;

	private StopWatch stopwatch = new StopWatch();

	public int getTimeoutMS() {
		return sim.getRuntime(src).getLockWaitTimeout();
	}

	public SimRound(Address src, Simulator sim) {
		this(src, sim, 0L);
	}

	public SimRound(Address src, Simulator sim, long sleepTime) {
		this.sim = sim;
		this.src = src;
		this.round = this;
		// schedule lock acquisition for beginning of round
		sim.scheduleRelative(new AcquireSrcLock(), sleepTime);
	}

	private void cancel() {
		System.out.printf("     --------------> cancel round at %s\n", src);
		cancelled = true;
	}

	public void abortRound(long offsetTime) {
		assert (!cancelled); // no reason we should be cancelled more than once

		cancel();

		if (haveLockSrc) {
			sim.scheduleRelative(new ReleaseSrcLock(), offsetTime);
		}
		if (haveLockDst) {
			sim.scheduleRelative(new ReleaseDstLock(), offsetTime);
		}
		reschedule(sim.getRuntime(src).getInterval() + offsetTime);
	}

	public class RoundEvent extends SimulatorEvent {

		@Override
		public boolean isCancelled() {
			return super.isCancelled() || round.cancelled;
		}

		public RoundEvent(Address src) {
			super(src);
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			// do nothing here
		}

		@Override
		public void abortRound(Simulator simulator) {
			round.abortRound(sim.getRuntime(src).getLockWaitTimeout());
		}

		@Override
		public void fatalErrorHalt(Simulator simulator) {
			round.abortRound(sim.getRuntime(src).getLockWaitTimeout());
			super.fatalErrorHalt(simulator);
		}
	}

	public class AcquireSrcLock extends AcquireLock {
		public AcquireSrcLock() {
			super(round.src, round.src, new SelectPhase(round.src), null);
		}

		@Override
		public void onAcquireLock() {
			haveLockSrc = true;
		}

		@Override
		public void onTimeout() throws MicaRuntimeException {
			sim.getRuntime(getSrc()).handleError(INITIATOR_LOCK_TIMEOUT);
		}
	}

	public abstract class AcquireLock extends RoundEvent {

		private TimeoutEvent timeout = null;

		private Address lock = null;
		private RoundEvent continuation = null;
		private String timeoutErrorMsg = null;

		public AcquireLock(Address src, Address lock, RoundEvent continuation,
				String timeoutErrorMsg) {
			super(src);
			this.lock = lock;
			this.continuation = continuation;
			this.timeoutErrorMsg = timeoutErrorMsg;
		}

		public void onTimeout() throws MicaRuntimeException {
			logJson(getSrc(), timeoutErrorMsg, null);
		}

		public abstract void onAcquireLock();

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			if (simulator.lock(lock, getSrc())) {
				if (timeout != null) {
					timeout.cancel();
				}
				// got the lock!
				onAcquireLock();
				simulator.scheduleRelative(continuation, 0);
			} else {
				// failed to get lock
				simulator.addLockWaiter(lock, this);

				assert (timeout == null); // weirdness is happening if timeout
											// is non-null
				if (timeout == null) {
					timeout = new TimeoutEvent(src, this);
					simulator.scheduleRelative(timeout, getTimeoutMS());
				}
			}
		}

		@Override
		public String toString() {
			return super.toString() + " " + String.format("lock:%s", lock);
		}
	}

	public class AcquireDstLock extends AcquireLock {
		public AcquireDstLock() {
			super(round.src, round.dst, new GossipPhase(round.src),
					"mica-error-accept-connection");
		}

		@Override
		public void onAcquireLock() {
			haveLockDst = true;
		}

	}

	public class ReleaseDstLock extends SimulatorEvent {// not a RoundEvent ...
		// we don't want
		// round
		// cancellation to
		// cancel this

		public ReleaseDstLock() {
			super(round.dst);
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			if (haveLockDst) {
				simulator.unlock(round.dst, round.src); // getSrc() is actually
														// round.dst;
				// see
				// constructor
				haveLockDst = false;
			}
		}

		@Override
		public String toString() {
			return super.toString() + " " + String.format("lock:%s", round.dst);
		}
	}

	public class ReleaseSrcLock extends SimulatorEvent { // not a RoundEvent ...
															// we don't want
															// round
															// cancellation to
															// cancel this

		public ReleaseSrcLock() {
			super(round.src);
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			if (haveLockSrc) {
				simulator.unlock(round.src, round.src);
				haveLockSrc = false;
			}
		}

		@Override
		public String toString() {
			return super.toString() + " " + String.format("lock:%s", round.src);
		}
	}

	public class GossipPhase extends RoundEvent {

		public GossipPhase(Address src) {
			super(src);
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			// should have both locks by this point

			SimRuntime<?> rta = simulator.getRuntime(round.src);
			SimRuntime<?> rtb = simulator.getRuntime(round.dst);

			Protocol a = rta.getProtocolInstance();
			Protocol b = rtb.getProtocolInstance();

			simulator.setRuntime(rta);
			simulator.setRuntime(rtb);

			stopwatch.reset();

			try {
				a.update(b);
			} catch (Throwable t) {
				// FIXME HANDLE UPDATE ERROR
				// HANDLE ERROR MORE GRACEFULLY --- runtime exception is
				// temporary for debugging
				throw new RuntimeException(t);
				// throw new AbortRound();
			}

			long completionTimeRemote = stopwatch.elapsed();

			rta.logState("gossip-initiator");
			rtb.logState("gossip-receiver");

			simulator.scheduleRelative(new ReleaseDstLock(),
					completionTimeRemote);

			// run post-update
			simulator.setRuntime(rta);
			try {
				rta.getProtocolInstance().postUpdate();
			} catch (Throwable t) {
				// FIXME HANDLE POST-UDPATE ERROR
				throw new AbortRound();
			}
			rta.logState("preupdate");

			simulator.setRuntime(rta);
			double rate = 0;
			try {
				rate = a.getRate();
			} catch (Throwable t) {
				// FIXME HANDLE RATE ERROR
				throw new AbortRound();
			}

			logJson(round.src, "mica-rate", rate);

			int interval = simulator.getRuntime(round.src).getInterval();

			long completionTimeLocal = stopwatch.elapsed();
			simulator.scheduleRelative(new ReleaseSrcLock(),
					completionTimeLocal);

			long sleepMs = (long) (((double) interval) / rate);

			reschedule(sleepMs + completionTimeLocal);
		}
	}

	protected void logJson(Address source, String msgType, Object payload) {
		((BaseProtocol) sim.getRuntime(source).getProtocolInstance()).logJson(
				msgType, payload);
	}

	public void reschedule(long sleepMs) {
		new SimRound(src, sim, sleepMs);
	}

	public class SelectPhase extends RoundEvent {
		public SelectPhase(Address src) {
			super(src);
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			SimRuntime<?> rta = simulator.getRuntime(getSrc());
			simulator.setRuntime(rta);

			stopwatch.reset();

			SelectEvent se = rta.select();
			logJson(getSrc(), "mica-select", se);

			round.dst = se.selected;
			long t = stopwatch.elapsed();

			if (dst != null) {
				simulator.scheduleRelative(new AcquireDstLock(), t);
			}

			// run pre-update
			simulator.setRuntime(rta);
			try {
				rta.getProtocolInstance().preUpdate(round.dst);
			} catch (Throwable th) {
				// FIXME HANDLE ERROR CORRECTLY FROM PRE-UPDATE
				throw new AbortRound();
			}
			rta.logState("preupdate");

		}

	}

	public class TimeoutEvent extends RoundEvent {
		private AcquireLock onTimeoutCallback = null;

		public TimeoutEvent(Address a, AcquireLock onTimeoutCallback) {
			super(a);
			this.onTimeoutCallback = onTimeoutCallback;
		}

		@Override
		public void execute(Simulator simulator) throws MicaRuntimeException {
			round.abortRound(1);
			if (onTimeoutCallback != null)
				onTimeoutCallback.onTimeout();
		}

	}

}
