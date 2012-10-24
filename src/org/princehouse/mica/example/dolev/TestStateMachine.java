package org.princehouse.mica.example.dolev;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

/**
 * a simple state machine protocol for testing (much simpler than the full pulse
 * protocol)
 * 
 * @author lonnie
 * 
 */
public class TestStateMachine extends PulseStateMachine {

	private final static String RED = "RED";
	private final static String GREEN = "GREEN";

	public static List<PulseTransitionRule> transitions = Functional
			.list(new PulseTransitionRule[] {
					new PulseTransitionRule("red->green") {
						@Override
						public boolean ready(PulseStateMachine node) {
							return node.getState().equals(RED);
						}

						@Override
						public void apply(PulseStateMachine node) {
							node.setState(GREEN);
						}
					}, new PulseTransitionRule("green->red") {
						@Override
						public boolean ready(PulseStateMachine node) {
							return node.getState().equals(GREEN);
						}
						@Override
						public void apply(PulseStateMachine node) {
							node.setState(RED);
						}
					} }

			);

	private static final long serialVersionUID = 1L;

	public TestStateMachine(List<Address> neighbors, int f) {
		super(neighbors, f, RED);
	}

}
