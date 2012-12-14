package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.util.Distribution;

/**
 * Merge two protocols so that they make independent gossip choices.
 * Individual subprotocol select distributions are preserved.
 * Merged rate is <= sum of the rates of the two subprotocols.
 *
 * @author lonnie
 *
 */
public class MergeIndependent extends MergeBase {


	/**
	 * Constructor to create independent merged p1 + p2
	 * @param p1 First subprotocol
	 * @param p2 Second subprotocol
	 */
	public MergeIndependent(Protocol p1, Protocol p2) {
		super(p1,p2);
	}

	public MergeIndependent() {
		super();
	}
	
	/**
	 * Composite select distribution
	 * 
	 * @return
	 */
	@View
	public Distribution<Address> select() {
		double rate1 = getP1().getRate();
		double rate2 = getP2().getRate();
		
		
		double w = rate1 / (rate1 + rate2);
		Distribution<Address> d1 = getP1().getView();
		Distribution<Address> d2 = getP2().getView();
		
		// handle cases where one or both protocols are not gossiping
		if(d1 == null) {
			if(d2 == null) {
				return null;
			} else {
				return d2;
			}
		} else if(d2 == null) {
			return d1;
		}
		
		d1 = d1.scale(w);
		d2 = d2.scale(1-w);
		
		return d1.add(d2);
	}

	/**
	 * Composite rate function
	 * 
	 */
	@GossipRate
	public double mergedRate() {
		double rate1 = getP1().getRate();
		double rate2 = getP2().getRate();

		Distribution<Address> d1 = getP1().getView();
		Distribution<Address> d2 = getP2().getView();

	
		if(d1 == null) {
			if(d2 == null) {
				return 0;
			} else {
				return rate2;
			}
		} else if(d2 == null) {
			return rate1;
		}

		
		return rate1 + rate2;
	}


	/**
	 * 
	 * @param x  Gossip partner chosen by the runtime
	 * @param rng Random number generator
	 * @return
	 */
	@Override
	public Distribution<MergeSelectionCase> decideSelectionCase(Address x) {
		double rate1 = getP1().getRate();
		double rate2 = getP2().getRate();
		double w = rate1 / (rate1 + rate2);

		Distribution<Address> d1 = getP1().getView();
		Distribution<Address> d2 = getP2().getView();

		Distribution<MergeSelectionCase> outcomes = Distribution.create();		

		
		// handle cases where one both protocols don't gossip 
		if(d1 == null) {
			if(d2 == null) {
				outcomes.put(MergeSelectionCase.NEITHER, 1.0);
				return outcomes;
			} else {
				outcomes.put(MergeSelectionCase.P2, 1.0);
				return outcomes;
			}
		} else if(d2 == null) {
			outcomes.put(MergeSelectionCase.P1, 1.0);
			return outcomes;
		}
		
		double a = d1.get(x) * w;
		double b = d2.get(x) * (1-w);

		if (a < 1e-5 && b < 1e-5) {
			System.err
			.printf("Broken component distribution diagnostic for x=%s: d1=%s %s, d2=%s %s\n",
					x, a, (d1.containsKey(x) ? "" : "(MISSING)"), b,
					(d2.containsKey(x) ? "" : "(MISSING)"));
			d1.dump(System.err);
			d2.dump(System.err);
			throw new RuntimeException("broken component distributions");
		}

		double alpha = a / (a+b);
		double beta = b / (a+b);
		outcomes.put(MergeSelectionCase.P1, alpha);
		outcomes.put(MergeSelectionCase.P2, beta);
		return outcomes;
	}

	/**
	 * Convenient static method for merging p1+p2.  Same as 
	 *   "new MergeIndependent(p1,p2)"
	 *   
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static MergeIndependent merge(Protocol p1, Protocol p2) {
		return new MergeIndependent(p1,p2);
	}

	private static final long serialVersionUID = 1L;
	
	public static MergeOperator operator = new MergeOperator() {
		@Override
		public MergeBase merge(BaseProtocol p1, BaseProtocol p2) {
			return new MergeIndependent(p1,p2);
		}
	};
}
