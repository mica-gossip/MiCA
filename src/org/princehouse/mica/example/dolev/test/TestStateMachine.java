package org.princehouse.mica.example.dolev.test;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.dolev.LogStructuredStateMachine;
import org.princehouse.mica.example.dolev.PulseMessage;
import org.princehouse.mica.example.dolev.PulseTransitionRule;
import org.princehouse.mica.util.Functional;

/**
 * protocol)
 * a simple state machine protocol for testing (much simpler than the full pulse
 * 
 * @author lonnie
 * 
 */
public class TestStateMachine extends LogStructuredStateMachine {

	@Override
	public int settingsTransitionLimit() { return 1; }
	@Override
	public boolean settingsTransitionForbidRepeat() { return true; }

	private final static String RED = "RED";
	private final static String GREEN = "GREEN";

	public String color = null;
	
	public static List<PulseTransitionRule> transitions = Functional
			.list(new PulseTransitionRule[] {
					new PulseTransitionRule("red->green") {
						@Override
						public boolean ready(LogStructuredStateMachine node) {
							PulseMessage m = node.getState();
							Object state = m.state;
							boolean r = RED.equals(state);
							node.logJson("lssm-test-red-green",String.format("current state:%s ready:%s",state,r));
							return r;
						}
						@Override
						public void apply(LogStructuredStateMachine node) {
							node.setState(GREEN);
						}
					}, new PulseTransitionRule("green->red") {
						@Override
						public boolean ready(LogStructuredStateMachine node) {
							PulseMessage m = node.getState();
							Object state = m.state;
							boolean r = GREEN.equals(state);
							node.logJson("lssm-test-green-red",String.format("current state:%s ready:%s",state,r));
							return r;
						}
						@Override
						public void apply(LogStructuredStateMachine node) {
							node.setState(RED);
						}
					} }

			);

	private static final long serialVersionUID = 1L;

	public TestStateMachine(List<Address> neighbors, int f) {
		super(neighbors, f, RED);
	}

	@Override
	public List<PulseTransitionRule> getTransitions() {
		return transitions;
	}
	
	@Override
	public void postUpdate() {
		this.logJson("pulse-test-logsize",getLog().size());
		super.postUpdate();
	}
	
	@Override 
	public void setState(Object state) {
		color = (String) state;
		super.setState(state);
	}
}
