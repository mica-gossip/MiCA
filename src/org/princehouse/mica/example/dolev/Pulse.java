package org.princehouse.mica.example.dolev;

import java.util.List;
import java.util.Set;

import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Array;
import org.princehouse.mica.util.Functional;
import static org.princehouse.mica.example.dolev.PulseState.*;

public class Pulse extends LogStructuredStateMachine {

	public static class PulseTransitionRule extends LSSMTransitionRule {
		private Set<PulseState> src = null;
		private PulseState dst = null;

		public PulseTransitionRule(PulseState[] src, PulseState dst) {
			super(String.format("%s -> %s", Array.join(",", src), dst));
			this.src = Functional.set(Functional.list(src));
			this.dst = dst;
		}

		public PulseTransitionRule(PulseState src, PulseState dst) {
			this(new PulseState[] { src }, dst);
		}

		@Override
		public boolean ready(LogStructuredStateMachine node) {
			LSSMMessage s = node.getState();
			if (!src.contains(s.state)) {
				return false;
			}
			Pulse p = (Pulse) node;
			return readyPulse(p);
		}

		// override this
		public boolean readyPulse(Pulse node) {
			return true;
		}

		@Override
		public void apply(LogStructuredStateMachine node) {
			Pulse p = (Pulse) node;
			applyPulse(p);
			p.setState(dst);
		}

		// override this
		public void applyPulse(Pulse node) {
			// manipulation of log should go here.
			// executed before new state is set
		}

	};

	public static List<LSSMTransitionRule> transitions = Functional
			.list(new LSSMTransitionRule[] {
					// third-level transitions
					new PulseTransitionRule(WAIT, COLLECT) {
						// TODO FIXME
					},
					new PulseTransitionRule(READY, PROPOSE) {
						// TODO FIXME
					},
					new PulseTransitionRule(DOORWAY, ENTRY) {
						// TODO FIXME
					},
					// second-level transitions
					new PulseTransitionRule(new PulseState[] { COLLECT, WAIT },
							PULSE) {
						// TODO FIXME
					},
					// top-level transitions
					new PulseTransitionRule(new PulseState[] { PULSE, COLLECT,
							WAIT }, READY) {
						// TODO FIXME
					},
					new PulseTransitionRule(new PulseState[] { RECOVER }, READY) {
						// TODO FIXME
					},
					new PulseTransitionRule(
							new PulseState[] { READY, PROPOSE }, DOORWAY) {
						// TODO FIXME
					},
					new PulseTransitionRule(
							new PulseState[] { DOORWAY, ENTRY }, WAIT) {
						// TODO FIXME
					},
					new PulseTransitionRule(
							new PulseState[] { DOORWAY, ENTRY }, RECOVER) {
						// TODO FIXME
					},
					new PulseTransitionRule(new PulseState[] { PULSE, COLLECT,
							WAIT }, RECOVER) {
						// TODO FIXME
					},

			});

	private static final long serialVersionUID = 1L;

	protected int d, T1, T2, T3, T4;

	// d, T1-T4 measured in rounds

	public Pulse(Overlay overlay, int n, int f, int T1, int T2, int T3, int T4,
			int d) {
		super(overlay, n, f, READY);
		this.d = d;
		this.T1 = T1;
		this.T2 = T2;
		this.T3 = T3;
		this.T4 = T4;
		assert (T4 > 3 * T1 + 5 * d); // from protocol desc. p.2
	}

	@Override
	public List<LSSMTransitionRule> getTransitions() {
		return transitions;
	}
	
	public PulseState state;
	//public int pulseRound = 0;
	
	@Override
	public void doRound() {
		super.doRound();
		//pulseRound = getRound();
		state = (PulseState) getState().state;
	}

}
