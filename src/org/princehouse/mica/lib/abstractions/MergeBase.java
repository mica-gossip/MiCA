package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;
/**
 * Merge two protocols in such a way that the composite protocol gossips both
 * subprotocols to the same target peer as frequently as possible, while still
 * respecting individual address distributions.
 * 
 * NOTE: This is called "general merge" in the MiCA paper.
 * 
 * @author lonnie
 * 
 */
public abstract class MergeBase extends BaseProtocol {

	private Protocol p1;
	
	private MergeSelectionCase subProtocolGossipCase = MergeSelectionCase.NA;
	
	protected MergeSelectionCase getSubProtocolGossipCase() {
		return subProtocolGossipCase;
	}

	protected void setSubProtocolGossipCase(MergeSelectionCase subProtocolGossipCase) {
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
	 * @param p1 First subprotocol
	 * @param p2 Second subprotocol
	 */
	public MergeBase(Protocol p1, Protocol p2) {
		setP1(p1);
		setP2(p2);
	}

	public MergeBase() {
		this(null, null);
	}



	/**
	 * Composite update function.
	 * Run both sub-updates if possible; otherwise run one or the other
	 * 
	 * @param that
	 */
	@GossipUpdate
	public void update(MergeBase that) {
		switch (getSubProtocolGossipCase()) {
		case P1:
			// only protocol 1 gossips
			getP1().executeUpdate(that.getP1());
			break;
		case P2:
			// only protocol 2 gossips
			getP2().executeUpdate(that.getP2());
			break;
		case BOTH:
			// both protocols gossip
			getP1().executeUpdate(that.getP1());
			getP2().executeUpdate(that.getP2());
			break;
		case NA:
			throw new RuntimeException("Merge error: No selection choice! Did you override preUpdate and forget to call super()?");
		case NEITHER:
			// nobody gossips
			break;
		}
	}

	/**
	 * Note: If preUpdate is overridden and this super method never called, merge will break
	 */
	@Override
	public void preUpdate(Address selected) {
		setSubProtocolGossipCase(decideSelectionCase(selected).sample(getRuntimeState().getRandom()));
		logJson("merge-choose-subprotocol",getSubProtocolGossipCase());
		switch(getSubProtocolGossipCase()) {
		case P1:
			getP1().preUpdate(selected);
			break;
		case P2:
			getP2().preUpdate(selected);
			break;
		case BOTH:
			getP1().preUpdate(selected);
			getP2().preUpdate(selected);
			break;
		case NA:
			throw new RuntimeException("subProtocolGossipCase is NA, which should be impossible");
		}

	}
		
	@Override
	public void postUpdate() {
		switch(getSubProtocolGossipCase()) {
		case P1:
			getP1().postUpdate();
			break;
		case P2:
			getP2().postUpdate();
			break;
		case BOTH:
			getP1().postUpdate();
			getP2().postUpdate();
			break;
		}
	}
	
	/**
	 * 
	 * @param x  Gossip partner chosen by the runtime
	 * @param rng Random number generator
	 * @return
	 */
	public abstract Distribution<MergeSelectionCase> decideSelectionCase(Address x);
	
	
	private static final long serialVersionUID = 1L;
}
