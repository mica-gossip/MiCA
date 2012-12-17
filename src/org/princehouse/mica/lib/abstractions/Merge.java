package org.princehouse.mica.lib.abstractions;

import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

import fj.F;
import fj.P;
import fj.P2;

/**
 * 
 * Serves as a base class for different merge operators. Cannot be instantiated
 * by itself.
 * 
 * Merge two protocols in such a way that the composite protocol gossips both
 * subprotocols to the same target peer as frequently as possible, while still
 * respecting individual address distributions.
 * 
 * NOTE: This is called "general merge" in the MiCA paper.
 * 
 * @author lonnie
 * 
 */
public abstract class Merge extends BaseProtocol {

	private Protocol p1;

	// Note: can't be transient; needed for postUpdate, and a transient value
	// would be erased by serialization
	private MergeSelectionCase subProtocolGossipCase = MergeSelectionCase.NA;

	protected MergeSelectionCase getSubProtocolGossipCase() {
		return subProtocolGossipCase;
	}

	@Override
	public void unreachable(Address peer) {
		// FIXME should this get called even for subprotocols that didn't gossip to peer??
		if(getP1() != null) getP1().unreachable(peer);
		if(getP2() != null) getP2().unreachable(peer);
	}
	
	@Override 
	public void busy(Address peer) {
		// FIXME should this get called even for subprotocols that didn't gossip to peer??
		if(getP1() != null) getP1().busy(peer);
		if(getP2() != null) getP2().busy(peer);
	}
	
	protected void setSubProtocolGossipCase(
			MergeSelectionCase subProtocolGossipCase) {
		this.subProtocolGossipCase = subProtocolGossipCase;
	}

	/**
	 * Get first subprotocol
	 * 
	 * @return
	 */
	public Protocol getP1() {
		return p1;
	}

	/**
	 * Set first subprotocol
	 * 
	 * @param p1
	 */
	public void setP1(Protocol p1) {
		this.p1 = p1;
	}

	/**
	 * Get second subprotocol
	 * 
	 * @return
	 */
	public Protocol getP2() {
		return p2;
	}

	/**
	 * Set second subprotocol
	 * 
	 * @param p2
	 */
	public void setP2(Protocol p2) {
		this.p2 = p2;
	}

	private Protocol p2;

	/**
	 * Constructor to make composite protocol p1 + p2
	 * 
	 * @param p1
	 *            First subprotocol
	 * @param p2
	 *            Second subprotocol
	 */
	public Merge(Protocol p1, Protocol p2) {
		setP1(p1);
		setP2(p2);
	}

	public Merge() {
		this(null, null);
	}

	/**
	 * For logging / debugging only. Assign a name to P1
	 * 
	 * @return
	 */
	public String getP1Name() {
		return "p1";
		//if (getP1() == null)
		//	return "null";
		//else
		//	return getP1().getClass().getSimpleName();
	}

	/**
	 * For logging / debugging only. Assign a name to P2
	 * 
	 * @return
	 */
	public String getP2Name() {
		return "p2";
//		if (getP2() == null)
//			return "null";
//		else
//			return getP2().getClass().getSimpleName();
	}

	/**
	 * Composite update function. Run both sub-updates if possible; otherwise
	 * run one or the other
	 * 
	 * @param that
	 */
	@GossipUpdate
	@Override
	public void update(Protocol p) {
		Merge that = (Merge) p;
		executeUpdateMerge(that, true);

	}

	/**
	 * Execute the update method of a subprotocol
	 * 
	 * @param i
	 * @param r
	 */
	private void executeSubprotocolUpdate(Protocol i, Protocol r) {
		if (i instanceof Merge) {
			((Merge) i).executeUpdateMerge((Merge) r, false);
		} else {
			i.update(r);
		}
	}

	/**
	 * Called to execute the update of MergeBase subclasses, including any
	 * merged subprotocols that inherit MergeBase Includes a parameter for
	 * writing to the log. We only want the top composite protocol to write to
	 * the log
	 * 
	 * @param that
	 * @param writeLog
	 */
	private void executeUpdateMerge(Merge that, boolean writeLog) {
		switch (getSubProtocolGossipCase()) {
		case P1:
			// only protocol 1 gossips
			executeSubprotocolUpdate(getP1(), that.getP1());
			break;
		case P2:
			// only protocol 2 gossips
			executeSubprotocolUpdate(getP2(), that.getP2());
			break;
		case BOTH_P1P2:
			// both protocols gossip
			executeSubprotocolUpdate(getP1(), that.getP1());
			executeSubprotocolUpdate(getP2(), that.getP2());
			break;
		case BOTH_P2P1:
			// both protocols gossip
			executeSubprotocolUpdate(getP2(), that.getP2());
			executeSubprotocolUpdate(getP1(), that.getP1());
			break;
		case NA:
			throw new RuntimeException(
					"Merge error: No selection choice! Did you override preUpdate and forget to call super()?");
		case NEITHER:
			// nobody gossips
			break;
		}

		if (writeLog && LogFlag.merge.test()) {
			List<P2<String, Boolean>> leaves = collectLeafProtocolStatus();
			String outstr = "";
			int i = 0;
			for (P2<String, Boolean> temp : leaves) {
				if (i++ > 0)
					outstr += ",";
				outstr += String.format("%s:%s", temp._1(), temp._2());
			}

			logJson(LogFlag.merge, "merge-execute-subprotocols", outstr);
		}
	}

	/**
	 * For logging / debugging only
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<P2<String, Boolean>> collectLeafProtocolStatus() {
		List<P2<String, Boolean>> status = Functional.list();
		MergeSelectionCase choice = getSubProtocolGossipCase();

		// given a pair (name:string,status:boolean), return (name,false)
		F<P2<String, Boolean>, P2<String, Boolean>> setFalse = new F<P2<String, Boolean>, P2<String, Boolean>>() {
			@Override
			public P2<String, Boolean> f(P2<String, Boolean> arg0) {
				return P.p(arg0._1(), false);
			}
		};

		// leaves of p1
		List<P2<String, Boolean>> temp = collectLeafProtocolStatus(getP1(),
				getP1Name());
		if (choice.p1Gossips()) {
			status = Functional.concatenate(status, temp);
		} else {
			status = Functional.concatenate(status,
					Functional.list(Functional.map(temp, setFalse)));
		}

		// leaves of p2
		temp = collectLeafProtocolStatus(getP2(), getP2Name());
		if (choice.p2Gossips()) {
			status = Functional.concatenate(status, temp);
		} else {
			status = Functional.concatenate(status,
					Functional.list(Functional.map(temp, setFalse)));
		}

		return status;
	}

	/**
	 * For logging / debugging only
	 * 
	 * @return
	 */
	private List<P2<String, Boolean>> collectLeafProtocolStatus(Protocol p,
			String pname) {
		if (p == null) {
			return Functional.list();
		} else if (p instanceof Merge) {
			return ((Merge) p).collectLeafProtocolStatus();
		} else {
			// p is a leaf
			return Functional.list(P.p(pname, true));
		}
	}

	/**
	 * Note: If preUpdate is overridden and this super method never called,
	 * merge will break
	 */
	@Override
	public void preUpdate(Address selected) {
		setSubProtocolGossipCase(decideSelectionCase(selected).sample(
				getRuntimeState().getRandom()));
		// logJson("merge-choose-subprotocol",getSubProtocolGossipCase());
		switch (getSubProtocolGossipCase()) {
		case P1:
			getP1().preUpdate(selected);
			break;
		case P2:
			getP2().preUpdate(selected);
			break;
		case BOTH_P1P2:
			getP1().preUpdate(selected);
			getP2().preUpdate(selected);
			break;
		case BOTH_P2P1:
			getP1().preUpdate(selected);
			getP2().preUpdate(selected);
			break;
		case NA:
			throw new RuntimeException(
					"subProtocolGossipCase is NA, which should be impossible");
		}

	}

	@Override
	public void postUpdate() {
		switch (getSubProtocolGossipCase()) {
		case P1:
			getP1().postUpdate();
			break;
		case P2:
			getP2().postUpdate();
			break;
		case BOTH_P1P2:
			getP1().postUpdate();
			getP2().postUpdate();
			break;
		case BOTH_P2P1:
			getP1().postUpdate();
			getP2().postUpdate();
			break;
		}
	}

	/**
	 * 
	 * @param x
	 *            Gossip partner chosen by the runtime
	 * @param rng
	 *            Random number generator
	 * @return
	 */
	public abstract Distribution<MergeSelectionCase> decideSelectionCase(
			Address x);

	private static final long serialVersionUID = 1L;
}
