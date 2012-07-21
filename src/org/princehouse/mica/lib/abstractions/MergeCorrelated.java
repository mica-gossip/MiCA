package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

// FIXME needs correct handling for null distributions of either or both constituents

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
public class MergeCorrelated extends MergeBase {

	/**
	 * Constructor to make composite protocol p1 + p2
	 * @param p1 First subprotocol
	 * @param p2 Second subprotocol
	 */
	public MergeCorrelated(Protocol p1, Protocol p2) {
		super(p1,p2);
	}

	public MergeCorrelated() {
		super();
	}

	/**
	 * Composite gossip rate; less than or equal to the sum of the subprotocol rates
	 * @return
	 */
	@GossipRate
	public double mergedRate() {
		Protocol p1 = getP1();
		Protocol p2 = getP2();
		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();
		double rate1 = p1.getFrequency();
		double rate2 = p2.getFrequency();
		return Distribution.max(d1.scale(rate1), d2.scale(rate2)).getSum();
	}

	/**
	 * Compute composite select distribution
	 * 
	 * @return composite select distribution
	 */
	@Select
	public Distribution<Address> select() {
		double rate1 = getP1().getFrequency();
		double rate2 = getP2().getFrequency();

		double w = rate1 / (rate1 + rate2);
		Distribution<Address> d1 = getP1().getSelectDistribution().scale(w);
		Distribution<Address> d2 = getP2().getSelectDistribution().scale(1-w);
				
		return Distribution.max(d1, d2).ipnormalize();
	}
	
	/**
	 * 
	 * @param x  Gossip partner chosen by the runtime
	 * @param rng Random number generator
	 * @return
	 */
	@Override
	public Distribution<MergeSelectionCase> decideSelectionCase(Address x) {
		Distribution<Address> d1 = getP1().getSelectDistribution();
		Distribution<Address> d2 = getP2().getSelectDistribution();

		double rate1 = getP1().getFrequency();
		double rate2 = getP2().getFrequency();

		double w = rate1 / (rate1 + rate2);

		double p1 = d1.get(x) * w;
		double p2 = d2.get(x) * (1-w);

		if (p1 < 1e-5 && p2 < 1e-5) {
			System.err
					.printf("Broken component distribution diagnostic for x=%s: p1=%s=%s %s, p2=d2=%s %s\n",
							x, getP1().getClass(), p1, (d1.containsKey(x) ? "" : "(MISSING)"), getP2().getClass(), p2,
							(d2.containsKey(x) ? "" : "(MISSING)"));
			d1.dump(System.err);
			d2.dump(System.err);
			throw new RuntimeException("broken component distributions");
		}

		double pmin = Math.min(p1,p2);
		double pmax = Math.max(p1,p2);
		
		double alpha = (p1 - pmin) / pmax;
		double beta = (p2 - pmin) / pmax;
		double gamma = pmin / pmax;
	
		Distribution<MergeSelectionCase> outcomes = new Distribution<MergeSelectionCase>();
		
		outcomes.put(MergeSelectionCase.P1, alpha);
		outcomes.put(MergeSelectionCase.P2, beta);
		outcomes.put(MergeSelectionCase.BOTH, gamma);

		return outcomes;
//		return outcomes.sample(rng);
	}
	

	/**
	 * Convenience static method for merging. Same as "new MergeCorrelated(p1,p2)"
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static MergeCorrelated merge(Protocol p1, Protocol p2) {
		return new MergeCorrelated(p1, p2);
	}
	
	
	
	private static final long serialVersionUID = 1L;
	
	public static MergeOperator operator = new MergeOperator() {
		@Override
		public MergeBase merge(BaseProtocol p1, BaseProtocol p2) {
			return new MergeCorrelated(p1,p2);
		}
	};
}
