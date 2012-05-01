package org.princehouse.mica.lib.abstractions;

import static org.princehouse.mica.util.Randomness.weightedChoice;

import java.util.Random;

import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

/**
 * Merge two protocols so that they make independent gossip choices.
 * Individual subprotocol select distributions are preserved.
 * Merged rate is <= sum of the rates of the two subprotocols.
 *
 * @author lonnie
 *
 */
public class MergeIndependent extends MergeAbstract {


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
	@Select
	public Distribution<Address> select() {

		Distribution<Address> d1 = getP1().getSelectDistribution();
		Distribution<Address> d2 = getP2().getSelectDistribution();

		if(d1.isEmpty() && d2.isEmpty()) {
			return null;
		}
		// d1 + d2 - d1 * d2
		return d1.add(d2).add(d1.multiply(d2));
	}

	/**
	 * Composite rate function
	 * 
	 */
	@GossipRate
	public double mergedRate() {
		Distribution<Address> d1 = getP1().getSelectDistribution();
		Distribution<Address> d2 = getP2().getSelectDistribution();
		double c = d1.multiply(d2).getSum();
		return (getP1().getFrequency() + getP2().getFrequency()) * (2.0 - c) / 2.0;
	}


	/**
	 * 
	 * @param x  Gossip partner chosen by the runtime
	 * @param rng Random number generator
	 * @return
	 */
	@Override
	public MergeSelectionCase decideSelectionCase(Address x, Random rng) {
		Distribution<Address> d1 = getP1().getSelectDistribution();
		Distribution<Address> d2 = getP2().getSelectDistribution();

		d1 = d1.copynormalize();
		d2 = d2.copynormalize();

		double a = d1.get(x);
		double b = d2.get(x);

		if (a < 1e-5 && b < 1e-5) {
			System.err
			.printf("Broken component distribution diagnostic for x=%s: d1=%s %s, d2=%s %s\n",
					x, a, (d1.containsKey(x) ? "" : "(MISSING)"), b,
					(d2.containsKey(x) ? "" : "(MISSING)"));
			d1.dump(System.err);
			d2.dump(System.err);
			throw new RuntimeException("broken component distributions");
		}

		double alpha = a * (1 - b) / (a + b - a * b);
		double beta = b * (1 - a) / (a + b - a * b);
		double gamma = a * b / (a + b - a * b);

		double[] weights = { alpha, beta, gamma };

		switch (weightedChoice(getRuntimeState().getRandom(), weights)) {
		case 0:
			return MergeSelectionCase.P1;
		case 1:
			return MergeSelectionCase.P2;
		case 2:
			return MergeSelectionCase.BOTH;
		default:
			throw new RuntimeException();
		}
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
}
