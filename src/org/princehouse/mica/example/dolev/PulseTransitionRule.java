package org.princehouse.mica.example.dolev;

import java.util.List;
import java.util.Set;

import org.princehouse.mica.util.Functional;

/**
 * PulseTransition is an abstract class that affects state transitions based on analysis 
 * of a list of messages.  The transition itself is a modification of that list of messages
 * @author lonnie
 *
 */
public abstract class PulseTransitionRule {
	private String name;
	
	public PulseTransitionRule(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract boolean ready(LogStructuredStateMachine node);
	
	public abstract void apply(LogStructuredStateMachine node);
}