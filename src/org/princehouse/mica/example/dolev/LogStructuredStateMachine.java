package org.princehouse.mica.example.dolev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public abstract class LogStructuredStateMachine extends PulseRoundManager {

	public int settingsTransitionLimit() { return Integer.MAX_VALUE; }
	public boolean settingsTransitionForbidRepeat() { return true; }
	
	public abstract List<PulseTransitionRule> getTransitions();

	/**
	 * log messages old than this will be purged default value = 5 rounds
	 */
	private int messageLifetimeMS;

	public int getMessageLifetimeMS() {
		return messageLifetimeMS;
	}

	public void setMessageLifetimeMS(int messageLifetimeMS) {
		this.messageLifetimeMS = messageLifetimeMS;
	}

	private static final long serialVersionUID = 1L;

	// kept sorted newest to oldest
	private List<PulseMessage> log = Functional.list();

	private Object defaultState = null;

	public LogStructuredStateMachine(List<Address> neighbors, int f,
			Object initialState) {
		super(neighbors, f);
		this.defaultState = initialState;
		setState(initialState);
		setDefaultMessageLifetimeMS();
	}

	private void setDefaultMessageLifetimeMS() {
		int intervalLengthMS = getRuntimeState().getIntervalMS();
		int nRoundsDefault = 5;
		double currentRate = getRate();
		setMessageLifetimeMS((int) (((double) intervalLengthMS) / currentRate * ((double) nRoundsDefault)));
	}

	/**
	 * Return the most recent state entry for a given address Returns the whole
	 * PulseMessage instance so that the timestamp can be inspected
	 * 
	 * @param address
	 * @param messages
	 * @return
	 */
	public PulseMessage getCurrentState(Address address,
			List<PulseMessage> messages) {
		for (PulseMessage m : messages) {
			if (m.peer.equals(address)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * shorthand for getState(this.getAddress(), this.getLog())
	 * 
	 * @return
	 */
	public PulseMessage getState() {
		return getCurrentState(getAddress(), getLog());
	}

	public List<PulseMessage> getLog() {
		return log;
	}

	public void setState(Object state) {
		Address addr = getAddress();
		assert (addr != null);

		log.add(0, new PulseMessage(getAddress(), getNow(), state,
				PulseMessage.MessageSource.ORIGIN));
	}

	private long getNow() {
		return (new Date()).getTime();
	}

	@GossipUpdate
	public void update(LogStructuredStateMachine that) {
		super.update(that);
		@SuppressWarnings("unchecked")
		List<PulseMessage> msgcopy = Functional.concatenate(that.log);
		// update both nodes
		that.assimilateInformation(getAddress(), log);
		assimilateInformation(that.getAddress(), msgcopy);
	}

	/**
	 * History is a map from address -> list of state change messages The list
	 * of states is sorted from new to old, so the first message represents
	 * current state.
	 * 
	 * Redundant messages are deleted: If there are two consecutive messages
	 * (m1,s1,new_timestamp,), (m2,s2,old_timestamp) are encountered, the new
	 * one is thrown out. The 'old' message is assigned the most direct source
	 * of the two. (FIXME: this is *probably* the correct behavior...)
	 * 
	 * @return
	 */
	private Map<Address, LinkedList<PulseMessage>> buildHistory() {
		// precondition: message list is sorted by timestamps descending (the
		// natural sort for pulsemessage)

		Map<Address, LinkedList<PulseMessage>> history = Functional.map();

		for (PulseMessage m : getLog()) {
			if (!history.containsKey(m.peer)) {
				LinkedList<PulseMessage> temp = new LinkedList<PulseMessage>();
				temp.addLast(m);
				history.put(m.peer, temp);
				continue;
			}

			LinkedList<PulseMessage> peerhist = history.get(m.peer);

			PulseMessage last = peerhist.getLast();
			if (!last.state.equals(m.state)) {
				// state change
				peerhist.addLast(m);
			} else {
				// same state. retain the older message
				if (m.source.compareTo(last.source) > 0) {
					// replace indirect with direct sources (verified working)
					//logJson("lssm-debug-swap", String.format(
					//		"replace origin %s with %s", m.source, last.source));
					m.source = last.source;
				}
				peerhist.pollLast();
				peerhist.addLast(m);
			}
		}
		return history;

	}

	/**
	 * precondition: log is sorted by ascending timestamp
	 */
	private void deleteRedundantMessages() {
		if (log.size() <= 1) {
			return;
		}

		int debug_ss = log.size();

		Map<Address, LinkedList<PulseMessage>> history = buildHistory();

		historyToLog(history);

		int debug_ns = log.size();

		logJson("lssm-debug-delete-redundant", String.format(
				"log redundancy size %s -> %s; history for %s nodes", debug_ss,
				debug_ns, history.size()));
	}

	/**
	 * rewrite the log using the given history
	 * @param history
	 */
	private void historyToLog(Map<Address, LinkedList<PulseMessage>> history) {

		log = new ArrayList<PulseMessage>();

		for (List<PulseMessage> peerhist : history.values()) {
			log = Functional.extend(log, peerhist);
		}
		Collections.sort(log);
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
		deleteRedundantMessages();

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
			logJson("lssm-ready");
			reset();
			doRound();
		} else {
			logJson("lssm-not-ready",
					String.format("waiting to hear from %s peers",
							this.getRemainingCount()));
		}

		// prevent our own state from expiring by re-adding it to the log if
		// it's getting old
		PulseMessage stateMsg = getState();
		if (stateMsg == null) {
			setState(defaultState);
		} else {
			long now = getNow();
			long age = now - stateMsg.timestamp;
			if (age > getMessageLifetimeMS() / 2) {
				setState(stateMsg.state);
			}
		}
	}

	// having received info from n-f peers, we attempt a state transition
	private void doRound() {
		// purgeExpiredMessages();
		final LogStructuredStateMachine thisFinal = this;
		int completedTransitions = 0;

		// To prevent infinite loops, do not allow the same transition to be
		// applied twice in one round
		final Set<PulseTransitionRule> applied = Functional.set();

		List<PulseTransitionRule> transitions = getTransitions();
		
		int limit = settingsTransitionLimit();
		
		while (true) {
			List<PulseTransitionRule> readyTransitions = Functional
					.list(Functional.filter(transitions,
							new F<PulseTransitionRule, Boolean>() {
								@Override
								public Boolean f(PulseTransitionRule t) {
									return !applied.contains(t)
											&& t.ready(thisFinal);
								}
							}));

			thisFinal.logJson("lssm-debug-transitions", String.format("transitions:%d ready:%d", transitions.size(), readyTransitions.size()));
			
			if (readyTransitions.size() > 1) {
				logJson("lssm-error-transition-conflict", String.format(
						"%d conflicting transitions; selecting first",
						readyTransitions.size()));
			} else if (readyTransitions.size() == 0) {
				break;
			}

			// readyTransitions == 1
			PulseTransitionRule t = readyTransitions.get(0);
			completedTransitions++;
			logJson("lssm-transition", t.getName());
			t.apply(this);
			
			if(settingsTransitionForbidRepeat()) {
				applied.add(t);
			}
			if(limit > 0 && completedTransitions >= limit) {
				break;
			}
		}

		if (completedTransitions == 0) {
			logJson("lssm-error-no-transitions");
		}
	}

	private void purgeExpiredMessages() {
		final long now = getNow();
		final int expiration = getMessageLifetimeMS();

		int s = log.size();
		log = Functional.list(Functional.filter(getLog(),
				new F<PulseMessage, Boolean>() {
					@Override
					public Boolean f(PulseMessage m) {
						return (now - m.timestamp) < expiration;
					}
				}));

		s -= log.size();
		if (s > 0) {
			this.logJson("purged-log-messages", s);
		}
	}
}
