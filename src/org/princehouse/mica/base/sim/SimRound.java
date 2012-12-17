package org.princehouse.mica.base.sim;

import static org.princehouse.mica.base.RuntimeErrorCondition.INITIATOR_LOCK_TIMEOUT;

import java.io.Serializable;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaException;
import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Logging.SelectEvent;

public class SimRound {

	private Address src;
	private Address dst = null;
	private Simulator sim;
	private SimRound round;
	private boolean cancelled = false;
	private long roundStartTime = 0L;
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
		roundStartTime = sim.getClock() + sleepTime;
		sim.scheduleRelative(new AcquireSrcLock(), sleepTime);
	}

	private void cancel() {
		// System.out.printf("     --------------> cancel round at %s\n", src);
		cancelled = true;
	}

	public void abortRound(long releaseLockOffset) {
		assert (!cancelled); // no reason we should be cancelled more than once

		cancel();

		long clock = sim.getClock();

		if (haveLockSrc) {
			sim.scheduleRelative(new ReleaseSrcLock(), releaseLockOffset);
		}
		if (haveLockDst) {
			sim.scheduleRelative(new ReleaseDstLock(), releaseLockOffset);
		}

		SimRuntime rta = sim.getRuntime(src);
		rta.logJson(LogFlag.user, "notable-event-abort",
				MiCA.getOptions().expname);

		sim.getRuntimeContextManager().setNativeRuntime(rta);
		double rate = 1.0;
		try {
			rate = rta.getProtocolInstance().getRate();
		} catch (Throwable t) {
			rta.logJson(LogFlag.user, "notable-event-ratefail");
			// Suppress error; default interval will be used
		} finally {
			sim.getRuntimeContextManager().clear();
		}

		long abortedRoundElapsed = clock - roundStartTime;
		long interval = (long) (((double) rta.getInterval()) / rate);

		long normalTime = interval - abortedRoundElapsed;
		long lateTime = releaseLockOffset + 1;

		long sleepTime = normalTime;

		if (normalTime < lateTime) {
			rta.logJson(LogFlag.user, "notable-event-late",
					MiCA.getOptions().expname);
			sleepTime = lateTime;
		}

		reschedule(sleepTime);
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
		public void execute(Simulator simulator) throws MicaException {
			// do nothing here
		}

		@Override
		public void abortRound(Simulator simulator) {
			round.abortRound(0);
		}

		@Override
		public void fatalErrorHalt(Simulator simulator) {
			round.abortRound(0);
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
		public void onTimeout() throws MicaException {
			sim.getRuntime(getSrc()).handleError(INITIATOR_LOCK_TIMEOUT, null);
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

		public void onTimeout() throws MicaException {
			sim.getRuntimeContextManager().setNativeRuntime(
					sim.getRuntime(round.src));
			logJson(LogFlag.error, getSrc(), timeoutErrorMsg, null);
			sim.getRuntimeContextManager().clear();

		}

		public abstract void onAcquireLock();

		@Override
		public void execute(Simulator simulator) throws MicaException {
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
		public void execute(Simulator simulator) throws MicaException {
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
		public void execute(Simulator simulator) throws MicaException {
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
		public void execute(Simulator simulator) throws MicaException {
			// should have both locks by this point

			SimRuntime rta = simulator.getRuntime(round.src);
			SimRuntime rtb = simulator.getRuntime(round.dst);

			stopwatch.reset();

			CommunicationPatternAgent pattern = MiCA.getCompiler().compile(
					rta.getProtocolInstance());

			try {
				Serializable m1 = pattern.f1(rta);
				Serializable m2 = pattern.f2(rtb, m1);
				pattern.f3(rta, m2);

				simulator.getRuntimeContextManager().setNativeRuntime(rta);
				rta.logState("gossip-initiator");
				simulator.getRuntimeContextManager().clear();

				simulator.getRuntimeContextManager().setNativeRuntime(rtb);
				rtb.logState("gossip-receiver");
				simulator.getRuntimeContextManager().clear();

			} catch (Throwable t) {
				rta.handleError(RuntimeErrorCondition.UPDATE_EXCEPTION, t);
			} finally {
				sim.getRuntimeContextManager().clear();
			}

			long completionTimeRemote = (MiCA.getOptions().simUpdateDuration < 0 ? stopwatch
					.elapsed() : MiCA.getOptions().simUpdateDuration);

			simulator.scheduleRelative(new ReleaseDstLock(),
					completionTimeRemote);

			// run post-update
			simulator.getRuntimeContextManager().setNativeRuntime(rta);
			try {
				rta.getProtocolInstance().postUpdate();
				rta.logState("preupdate");
			} catch (Throwable t) {
				rta.handleError(RuntimeErrorCondition.POSTUDPATE_EXCEPTION, t);
			} finally {
				simulator.getRuntimeContextManager().clear();
			}

			simulator.getRuntimeContextManager().setNativeRuntime(rta);
			double rate = 0;
			try {
				rate = rta.getProtocolInstance().getRate();
			} catch (Throwable t) {
				rta.handleError(RuntimeErrorCondition.RATE_EXCEPTION, t);
			} finally {
				simulator.getRuntimeContextManager().clear();

			}

			simulator.getRuntimeContextManager().setNativeRuntime(rta);
			logJson(LogFlag.rate, round.src, "mica-rate", rate);
			simulator.getRuntimeContextManager().clear();

			int interval = simulator.getRuntime(round.src).getInterval();

			long completionTimeLocal = stopwatch.elapsed();
			simulator.scheduleRelative(new ReleaseSrcLock(),
					completionTimeLocal);

			long sleepMs = (long) (((double) interval) / rate);

			long adjustedSleepTime = roundStartTime + sleepMs
					- (sim.getClock() + completionTimeLocal);

			reschedule(Math.max(0, adjustedSleepTime));
		}
	}

	protected void logJson(Object flags, Address source, String msgType,
			Object payload) {
		Runtime rt = sim.getRuntime(source);
		rt.getProtocolInstance().logJson(flags, msgType, payload);
	}

	public void reschedule(long sleepMs) {
		new SimRound(src, sim, sleepMs);
	}

	public SelectEvent select(Protocol p) throws FatalErrorHalt, AbortRound {
		SelectEvent se = null;
		try {
			se = new SelectEvent();
			se.selected = p.getView().sample(p.getRuntimeState().getRandom());
			if (se.selected.equals(p.getAddress())) {
				se.selected = null;
			}
		} catch (Throwable e) {
			sim.getRuntimeContextManager().getNativeRuntime()
					.handleError(RuntimeErrorCondition.SELECT_EXCEPTION, e);
		}
		return se;
	}

	public class SelectPhase extends RoundEvent {
		public SelectPhase(Address src) {
			super(src);
		}

		@Override
		public void execute(Simulator simulator) throws MicaException {
			SimRuntime rta = simulator.getRuntime(getSrc());
			simulator.getRuntimeContextManager().setNativeRuntime(rta);

			stopwatch.reset();
			SelectEvent se = null;

			try {
				se = select(rta.getProtocolInstance());
				logJson(LogFlag.select, getSrc(), "mica-select", se);
			} finally {
				simulator.getRuntimeContextManager().clear();

			}
			round.dst = se.selected;
			long t = stopwatch.elapsed();

			if (dst != null) {
				simulator.scheduleRelative(new AcquireDstLock(), t);
			}

			// run pre-update
			simulator.getRuntimeContextManager().setNativeRuntime(rta);
			try {
				rta.getProtocolInstance().preUpdate(round.dst);
				rta.logState("preupdate");
			} catch (Throwable th) {
				rta.handleError(RuntimeErrorCondition.POSTUDPATE_EXCEPTION, th);
			} finally {
				simulator.getRuntimeContextManager().clear();
			}

		}

	}

	public class TimeoutEvent extends RoundEvent {
		private AcquireLock onTimeoutCallback = null;

		public TimeoutEvent(Address a, AcquireLock onTimeoutCallback) {
			super(a);
			this.onTimeoutCallback = onTimeoutCallback;
		}

		@Override
		public void execute(Simulator simulator) throws MicaException {
			round.abortRound(1);
			if (onTimeoutCallback != null)
				onTimeoutCallback.onTimeout();
		}

	}

}
