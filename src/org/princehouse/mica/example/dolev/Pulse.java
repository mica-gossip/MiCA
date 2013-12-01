package org.princehouse.mica.example.dolev;

import static org.princehouse.mica.example.dolev.LSSMMessage.MessageSource.DIRECT;
import static org.princehouse.mica.example.dolev.LSSMMessage.MessageSource.INDIRECT;
import static org.princehouse.mica.example.dolev.PulseState.COLLECT;
import static org.princehouse.mica.example.dolev.PulseState.DOORWAY;
import static org.princehouse.mica.example.dolev.PulseState.ENTRY;
import static org.princehouse.mica.example.dolev.PulseState.PROPOSE;
import static org.princehouse.mica.example.dolev.PulseState.PULSE;
import static org.princehouse.mica.example.dolev.PulseState.READY;
import static org.princehouse.mica.example.dolev.PulseState.RECOVER;
import static org.princehouse.mica.example.dolev.PulseState.WAIT;

import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.example.dolev.LSSMMessage.MessageSource;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Array;
import org.princehouse.mica.util.Functional;

/*
 *
 * TODO 
 * 
 * 1. Custom behavior when "q in recover" message received:  Delete all older q messages for states other than recover
 * 2. Apply transition customs:
 *    "currently in" -> latest status arriving within the last 2d, DIRECT (??)
 *    "within T" - during the latest window of time of size T, the condition was satisfied. "This condition implies
 *                 waiting at most time T from moving into the relative state to see whether the condition holds.
 *                 the condition needs to continuously hold as long as the node stays in that state." 
 *    "in" - without window of time -- meaning had received it sinc ethe last time the specific state variable was reset
 *   "via gossip" - ...
 *   
 *   longest wait in /wait/ : 3d for nodes in pulse to notice a problem, +d for others to receive their switch to recover,
 *      +d for the node in wait to see all in wait
 *   
 */
public class Pulse extends LogStructuredStateMachine {

    public static class PulseTransitionRule extends LSSMTransitionRule {
        private Set<PulseState> src = null;

        public Set<PulseState> getSrc() {
            return src;
        }

        public boolean hasWaited(Pulse node, int ms) {
            return node.hasWaited(getSrc(), ms);
        }

        public void setSrc(Set<PulseState> src) {
            this.src = src;
        }

        public PulseState getDst() {
            return dst;
        }

        public void setDst(PulseState dst) {
            this.dst = dst;
        }

        private PulseState dst = null;
        private PulseState clear = null;

        public PulseTransitionRule(PulseState[] src, PulseState dst) {
            this(src, dst, null);
        }

        public PulseTransitionRule(PulseState[] src, PulseState dst, PulseState clear) {
            super(String.format("%s -> %s", Array.join(",", src), dst));
            this.src = Functional.set(Functional.list(src));
            this.dst = dst;
            this.clear = clear;
        }

        public PulseTransitionRule(PulseState src, PulseState dst) {
            this(new PulseState[] { src }, dst, null);
        }

        public PulseTransitionRule(PulseState src, PulseState dst, PulseState clear) {
            this(new PulseState[] { src }, dst, clear);
        }

        @Override
        final public boolean ready(LogStructuredStateMachine node) {
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
        final public void apply(LogStructuredStateMachine node) {
            Pulse p = (Pulse) node;
            if (clear != null) {
                p.clear(clear);
            }
            applyPulse(p);
            p.setState(dst);
        }

        // override this
        public void applyPulse(Pulse node) {
            // manipulation of log should go here.
            // executed before new state is set
            if (dst.equals(PulseState.PULSE)) {
                node.logJson(LogFlag.user, "pulse-pulse");
            }
        }

    };

    public static List<LSSMTransitionRule> transitions = Functional.list(new LSSMTransitionRule[] {
            // third-level transitions
            new PulseTransitionRule(WAIT, COLLECT) {

                @Override
                public boolean readyPulse(Pulse node) {
                    // 1) T3 and (>= n-f in WAIT via gossip)
                    return hasWaited(node, node.T3) && node.census(WAIT, INDIRECT) >= node.getN() - node.getF();
                }

            }, new PulseTransitionRule(READY, PROPOSE) {
                // 2) T4 or (>= f+1 in propose) or (>= n-f in PROPOSE U
                // RECOVER)
                @Override
                public boolean readyPulse(Pulse node) {
                    int f = node.getF();
                    int n = node.getN();

                    return hasWaited(node, node.T4) || node.census(PROPOSE, DIRECT) >= f + 1
                            || node.census(new PulseState[] { PROPOSE, RECOVER }, DIRECT) >= n - f;
                }
            }, new PulseTransitionRule(DOORWAY, ENTRY, RECOVER) {
                // 3) T1 [CLEAR RECOVER]
                @Override
                public boolean readyPulse(Pulse node) {
                    return hasWaited(node, node.T1);
                }

            },
            // second-level transitions
            new PulseTransitionRule(new PulseState[] { COLLECT, WAIT }, PULSE) {
                // 4) (>= n-f in COLLECT via gossip) or (>= f+1 in PULSE
                // within T3)
                @Override
                public boolean readyPulse(Pulse node) {
                    int n = node.getN();
                    int f = node.getF();
                    return node.census(COLLECT, INDIRECT) >= n - f || node.census(PULSE, DIRECT, node.T3) >= f + 1;
                }
            },
            // top-level transitions
            new PulseTransitionRule(new PulseState[] { PULSE, COLLECT, WAIT }, READY) {
                // 5) (>= n-f in PULSE within 3d) [CLEAR ALL]
                @Override
                public boolean readyPulse(Pulse node) {
                    int n = node.getN();
                    int f = node.getF();
                    return node.census(PULSE, DIRECT, node.d * 3) >= n - f;
                }

                @Override
                public void applyPulse(Pulse node) {
                    node.clearAll();
                    super.applyPulse(node);
                }
            }, new PulseTransitionRule(RECOVER, READY) {
                // 6) (>= n-f "currently on edge", i.e., in RECOVER???)
                // [CLEAR ALL]
                @Override
                public boolean readyPulse(Pulse node) {
                    int n = node.getN();
                    int f = node.getF();
                    return node.census(RECOVER, DIRECT) >= n - f;
                }

                @Override
                public void applyPulse(Pulse node) {
                    node.clearAll();
                    super.applyPulse(node);
                }
            }, new PulseTransitionRule(new PulseState[] { READY, PROPOSE }, DOORWAY) {
                // 7) (>= n-f in PROPOSE U DOORWAY via gossip)
                @Override
                public boolean readyPulse(Pulse node) {
                    int n = node.getN();
                    int f = node.getF();
                    return node.census(new PulseState[] { PROPOSE, DOORWAY }, INDIRECT) >= n - f;
                }
            }, new PulseTransitionRule(new PulseState[] { DOORWAY, ENTRY }, WAIT, PROPOSE) {
                // 8) (T3 and >= n-f in ENTRY U DOORWAY via gossip) or
                // (>= f+1 in COLLECT within T3) [CLEAR PROPOSE]
                @Override
                public boolean readyPulse(Pulse node) {
                    int n = node.getN();
                    int f = node.getF();
                    return (hasWaited(node, node.T3) && node.census(new PulseState[] { ENTRY, DOORWAY }, INDIRECT) >= n
                            - f)
                            || (node.census(COLLECT, DIRECT, node.T3) >= f + 1);
                }
            }, new PulseTransitionRule(new PulseState[] { DOORWAY, ENTRY }, RECOVER) {
                // 9) ELSE or (T1 and >= f+1 in RECOVER)
                @Override
                public boolean readyPulse(Pulse node) {
                    int f = node.getF();
                    return node.Else() || (hasWaited(node, node.T1) && node.census(RECOVER, DIRECT) >= f + 1);
                }
            }, new PulseTransitionRule(new PulseState[] { PULSE, COLLECT, WAIT }, RECOVER) {
                // 10) ELSE or (>= f+1 in RECOVER U PROPOSE)
                @Override
                public boolean readyPulse(Pulse node) {
                    int f = node.getF();
                    return node.Else() || node.census(new PulseState[] { RECOVER, PROPOSE }, DIRECT) >= f + 1;
                }
            },

    });

    private static final long serialVersionUID = 1L;

    public int d, T1, T2, T3, T4;

    // d, T1-T4 measured in rounds

    public Pulse(Overlay overlay, int n, int f, int T4, int d, Object initialState) {
        super(overlay, n, f, initialState);
        this.d = d; // time, in ms, within which all correct nodes should
                    // communicate with all other correct nodes.
                    // probably should be mica_interval * n + C as a default,
                    // where C is a fudge factor for round-trip gossip time
        this.T1 = 5 * d; // for others to catch up
        this.T2 = 3 * d; // for others to notice
        this.T3 = T1 + T2; // to wait for all
        this.T4 = T4; // a timeout to enable a window of time between pulses. T4
                      // > 3 * T1 + 5d (i.e., T4 > 20d)
        // steady state: T_pulse ~~ T4 + 5*T1 + 2d, or by default, about 50d
        assert (T4 > 3 * T1 + 5 * d); // from protocol desc. p.2
    }

    protected boolean Else() {
        // FIXME
        // this should check that current transition rule is being tested AFTER
        // all other transition rules from this source state that do not
        // invoke 'ELSE'. Currently we rely on correct ordering of the
        // transition rule definitions (rules are tested in the
        // order they are defined)
        return true;
    }

    protected int census(PulseState state, MessageSource source) {
        return census(new PulseState[] { state }, source);
    }

    protected int census(PulseState state, MessageSource source, int within) {
        return census(new PulseState[] { state }, source, within);
    }

    protected int census(PulseState[] states, MessageSource source) {
        return census(states, source, 0);
    }

    /**
     * Counts the number of nodes currently known to be in any of the given
     * states, excluding the local node. Only messages with >= source
     * attribution are considered
     * 
     * @param states
     * @param source
     * @param within
     *            Within "within" milliseconds of current time. Value of zero
     *            means that within is ignored and entire known history is used
     * @return
     */
    protected int census(PulseState[] states, MessageSource source, int within) {

        // TODO implement me
        return 0;
    }

    /**
     * Return true if current node has been in any combination of sourceStates
     * for the last t milliseconds
     * 
     * @param time
     * @return
     */
    protected boolean hasWaited(Set<PulseState> sourceStates, int t) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<LSSMTransitionRule> getTransitions() {
        return transitions;
    }

    public PulseState state;

    // public int pulseRound = 0;

    @Override
    public void doRound() {
        super.doRound();
        // pulseRound = getRound();
        state = (PulseState) getState().state;
    }

    public void clear(PulseState state) {
        // TODO: implement
    }

    public void clearAll() {
        for (PulseState state : PulseState.values()) {
            clear(state);
        }
    }
}
