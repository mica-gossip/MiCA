package org.princehouse.mica.example.dolev;

import java.util.List;

import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;

public class Pulse extends LogStructuredStateMachine {

	public static List<PulseTransitionRule> transitions = Functional
			.list(new PulseTransitionRule[] {
			// TODO
			/*
			 * new Transition("foo") {
			 * 
			 * @Override public boolean ready(List<Message> messages) { // TODO
			 * Auto-generated method stub return false; }
			 * 
			 * @Override public void apply(List<Message> messages) { // TODO
			 * Auto-generated method stub } }
			 */
			});

	private static final long serialVersionUID = 1L;

	private int d, T1, T2, T3, T4;

	// d, T1-T4 measured in rounds
	public Pulse(Overlay overlay, int d, int T1, int T2, int T3,
			int T4, int n, int f) {
		super(overlay, n, f, PulseState.ready);
		this.d = d;
		this.T1 = T1;
		this.T2 = T2;
		this.T3 = T3;
		this.T4 = T4;
		assert (T4 > 3 * T1 + 5 * d); // from protocol desc. p.2
	}

	@Override
	public List<PulseTransitionRule> getTransitions() {
		return transitions;
	}


}
