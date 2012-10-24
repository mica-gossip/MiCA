package org.princehouse.mica.example.dolev;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public class PulseStateMachine extends PulseRoundManager {

	public List<PulseTransitionRule> transitions;

	private static final long serialVersionUID = 1L;

	// kept sorted newest to oldest
	private List<PulseMessage> log = Functional.list();

	public PulseStateMachine(List<Address> neighbors, int f, Object initialState) {
		super(neighbors, f);	
		setState(initialState);
	}

	public Object getCurrentState(Address address, List<PulseMessage> messages) {
		for(PulseMessage m : messages) {
			if(m.peer.equals(address)) {
				return m.state;
			}
		}
		return null;
	}

	/**
	 * shorthand for getState(this.getAddress(), this.getLog())
	 * 
	 * @return
	 */
	public Object getState() {
		return getCurrentState(getAddress(), getLog());
	}
	
	public List<PulseMessage> getLog() {
		return log;
	}
	
	void setState(Object state) {
		Address addr = getAddress();
		assert(addr != null);
		
		log.add(0, new PulseMessage(getAddress(), getNow(), state, PulseMessage.MessageSource.ORIGIN));
	}
	
	private long getNow() {
		return (new Date()).getTime();
	}
	
	
	@GossipUpdate
	public void update(PulseStateMachine that) {
		super.update(that);
		@SuppressWarnings("unchecked")
		List<PulseMessage> msgcopy = Functional.concatenate(that.log);
		// update both nodes
		that.assimilateInformation(getAddress(), log);
		assimilateInformation(that.getAddress(), msgcopy);
	}

	private void deleteDuplicateMessages() {
		if(log.size() <= 1) {
			return;
		}
		
		// precondition: message list is sorted
		ListIterator<PulseMessage> li = log.listIterator();
	
		PulseMessage prev = li.next();
		// messages are considered equal (and redundant) if all fields except for source are equal.
		// direct sources given precedence over indirect [this is enforced by the sort order]
		while(li.hasNext()) {
			PulseMessage m = li.next();
			
			/*
			if(m == null) throw new RuntimeException();
			if(m.peer == null) throw new RuntimeException();
			if(m.state == null) throw new RuntimeException();
			if(prev == null) throw new RuntimeException();
			if(prev.peer == null) throw new RuntimeException();
			if(prev.state == null) throw new RuntimeException();
			*/
			
			if(m.timestamp == prev.timestamp && m.peer.equals(prev.peer) && m.state.equals(prev.state)) {
				li.remove();
			} else {
				prev = m;
			}
		}
	}
	
	private void assimilateInformation(Address source, List<PulseMessage> news) {
	
		// TODO combine messages
		// ...

		for (PulseMessage m : news) {
			// make indirect
			log.add(0, m.tell());
		}

		Collections.sort(log);

		// delete duplicate messages
		deleteDuplicateMessages();
		
		// special rules:
		// receving "recover N" for some node N deletes all previous N messages

		// latest status - current state of a node, message received within last
		// 2d
		// within T --- within a window of time T. must continuously state in
		// state for whole window T; T may have begun before moving to current
		// state
		// 'in' without window of time - meaning had received it since hte last
		// time the speicic state variable was reset
		// 'via gossip' -- indirect
		// longest wait: 3d (pulse to detect problem), +d for others to switch
		// to recover, +d for node in wait to see all in wait
		// one round == d time (during which all correct nodes talk with each
		// other)
	}

	@Override
	public void postUpdate() {
		if (ready()) {
			reset();
			doRound();
		}
	}

	// having received info from n-f peers, we attempt a state transition
	private void doRound() {
		purgeExpiredMessages();
		final PulseStateMachine thisFinal = this;
		int completedTransitions = 0;
		while (true) {
			List<PulseTransitionRule> readyTransitions = Functional.list(Functional
					.filter(transitions, new F<PulseTransitionRule, Boolean>() {
						@Override
						public Boolean f(PulseTransitionRule t) {
							return t.ready(thisFinal);
						}
					}));

			if (readyTransitions.size() > 1) {
				logJson("error-transition-conflict", String.format(
						"%d conflicting transitions; selecting first",
						readyTransitions.size()));
			} else if (readyTransitions.size() == 0) {
				break;
			}

			PulseTransitionRule t = readyTransitions.get(0);
			completedTransitions++;
			logJson("apply-transition", t.getName());
			t.apply(this);
		}

		if (completedTransitions == 0) {
			logJson("error-no-available-transitions");
		}
	}

	private void purgeExpiredMessages() {
		// TODO
		// ...
	}
}
