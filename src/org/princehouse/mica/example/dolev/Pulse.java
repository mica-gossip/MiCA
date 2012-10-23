package org.princehouse.mica.example.dolev;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public class Pulse extends BaseProtocol {

	public static class Message implements Comparable<Message> {
		public static enum MessageSource {
			ORIGIN, DIRECT, INDIRECT;
		};

		public Message(Address peer, long timestamp, PulseState state, MessageSource s) {
			this.peer = peer;
			this.timestamp = timestamp;
			this.source = s;
			this.state = state;
		}

		public Address peer;
		public long timestamp;
		public MessageSource source;
		public PulseState state;
		
		public Message tell() {
			// copy this message and add one level of indirection
			MessageSource s = null;
			switch (source) {
			case ORIGIN:
				s = MessageSource.DIRECT;
				break;
			case DIRECT:
				s = MessageSource.INDIRECT;
				break;
			case INDIRECT:
				s = MessageSource.INDIRECT;
				break;
			};
			
			return new Message(peer, timestamp, state, s);
		}

		@Override
		public int compareTo(Message other) {
			// sort from greatest to least time stamps
			int v = -(Long.valueOf(timestamp).compareTo(
					Long.valueOf(other.timestamp)));
			if(v != 0) 
				return v;
			// break ties by comparing directness.  More direct < less direct
			return source.compareTo(other.source);
		}
	}

	public static abstract class Transition {
		private Set<PulseState> sourceStates;
		private PulseState destState;
		public Transition(String name, PulseState[] appliesToStates, PulseState destState) {
			this.name = name;
			this.sourceStates = Functional.set(Functional.list(appliesToStates));// source states for the transition
			this.destState = destState;
		}
		public String name;
		
		public boolean ready(Pulse node, List<Message> messages) {
			PulseState currentState = node.getCurrentState(node.getAddress(), messages);
			return sourceStates.contains(currentState);
			
		}
		
		public void apply(Pulse node, List<Message> messages) {
			// If apply is overrridden, be sure to call this super method LAST
			node.setState(destState);
		}
	}

	public PulseState getCurrentState(Address address, List<Message> messages) {
		for(Message m : messages) {
			if(m.peer.equals(address)) {
				return m.state;
			}
		}
		return null;
	}
	
	public static List<Transition> transitions = Functional
			.list(new Transition[] {
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

	private List<Address> neighbors;
	private int cursor = 0; // cursor into neighbors; tells us whom to gossip
							// with next
	private int f = 0;
	private int n = 0;

	private List<Message> messages = Functional.list();

	private Set<Address> reached = Functional.set();

	// d, T1-T4 measured in rounds
	public Pulse(List<Address> neighbors, int d, int T1, int T2, int T3,
			int T4, int f) {
		this.d = d;
		this.T1 = T1;
		this.T2 = T2;
		this.T3 = T3;
		this.T4 = T4;

		assert (T4 > 3 * T1 + 5 * d); // from protocol desc. p.2

		this.neighbors = neighbors;
		this.f = f;
		this.n = neighbors.size() + 1; // precondition: our own address is not
										// in this list
		// start in ready state
		setState(PulseState.ready);
	}

	private void setState(PulseState state) {
		messages.add(0, new Message(getAddress(), getNow(), state, Message.MessageSource.ORIGIN));
	}
	private long getNow() {
		return (new Date()).getTime();
	}
	
	@View
	public Address nextPeer() {
		int s = neighbors.size();
		int start = cursor % s;
		Address p = neighbors.get(start);
		while (reached.contains(p)) {
			cursor++;
			if (start == cursor % s) {
				// we've gone all the way around
				return null; // could happen if all of our other neighbors talk
								// to us before we talk to them; in that case we
								// don't gossip
				// TODO: verify that postUpdate still gets called if we
				// null-gossip!!
			}
			p = neighbors.get(cursor % s);
		}
		return p;
	}

	@GossipUpdate
	public void update(Pulse that) {
		//long now = new Date().getTime();

		@SuppressWarnings("unchecked")
		List<Message> msgcopy = Functional.concatenate(that.messages);
		// update both nodes
		that.assimilateInformation(getAddress(), messages);
		assimilateInformation(that.getAddress(), msgcopy);

	}

	private void deleteDuplicateMessages() {
		if(messages.size() <= 1) {
			return;
		}
		
		// precondition: message list is sorted
		ListIterator<Message> li =messages.listIterator();
	
		Message prev = li.next();
		// messages are considered equal (and redundant) if all fields except for source are equal.
		// direct sources given precedence over indirect [this is enforced by the sort order]
		while(li.hasNext()) {
			Message m = li.next();
			if(m.timestamp == prev.timestamp && m.peer.equals(prev.peer) && m.state.equals(prev.state)) {
				li.remove();
			} else {
				prev = m;
			}
		}
	}
	
	private void assimilateInformation(Address source, List<Message> news) {
		if (reached.contains(source)) // ignore if we've already talked with
										// node this round
			return;
		reached.add(source);
		// TODO combine messages
		// ...

		for (Message m : news) {
			// make indirect
			messages.add(0, m.tell());
		}

		Collections.sort(messages);

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
		if (roundFinished()) {
			reached.clear();
			doRound();
		}
	}

	private boolean roundFinished() {
		return reached.size() >= (n - f);
	}

	// having received info from n-f peers, we attempt a state transition
	private void doRound() {
		purgeExpiredMessages();
		final Pulse thisFinal = this;
		int completedTransitions = 0;
		while (true) {
			List<Transition> readyTransitions = Functional.list(Functional
					.filter(transitions, new F<Transition, Boolean>() {
						@Override
						public Boolean f(Transition t) {
							return t.ready(thisFinal, messages);
						}
					}));

			if (readyTransitions.size() > 1) {
				logJson("error-transition-conflict", String.format(
						"%d competing transitions; selecting first",
						readyTransitions.size()));
			} else if (readyTransitions.size() == 0) {
				break;
			}

			Transition t = readyTransitions.get(0);
			completedTransitions++;
			t.apply(this, messages);
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
