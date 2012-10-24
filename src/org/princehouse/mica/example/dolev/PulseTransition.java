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
public abstract class PulseTransition {
	private Set<PulseState> sourceStates;
	
	private PulseState destState;
	
	public PulseTransition(String name, PulseState[] appliesToStates, PulseState destState) {
		this.name = name;
		this.sourceStates = Functional.set(Functional.list(appliesToStates));// source states for the transition
		this.destState = destState;
	}
	
	public String name;
	
	public boolean ready(Pulse node, List<PulseMessage> messages) {
		PulseState currentState = node.getCurrentState(node.getAddress(), messages);
		return sourceStates.contains(currentState);
		
	}
	
	public void apply(Pulse node, List<PulseMessage> messages) {
		// If apply is overrridden, be sure to call this super method LAST
		node.setState(destState);
	}
}