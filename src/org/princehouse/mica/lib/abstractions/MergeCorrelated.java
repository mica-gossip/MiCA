package org.princehouse.mica.lib.abstractions;

import static org.princehouse.mica.util.Randomness.weightedChoice;

import java.util.List;
import java.util.Random;

import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;

import fj.F2;

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
public class MergeCorrelated extends MergeAbstract {

	/**
	 * Constructor to make composite protocol p1 + p2
	 * @param p1 First subprotocol
	 * @param p2 Second subprotocol
	 */
	public MergeCorrelated(Protocol p1, Protocol p2) {
		super(p1,p2);
	}
	
	public MergeCorrelated(Protocol p1, Protocol p2, double p1share) {
		super(p1,p2,p1share);
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
		Distribution<Address> d1 = p1.getSelectDistribution().copynormalize();
		Distribution<Address> d2 = p2.getSelectDistribution().copynormalize();
		double c = Distribution.convolve(d1, d2,
				new F2<Double, Double, Double>() {
					@Override
					public Double f(Double a, Double b) {
						return Math.min(a, b);
					}
				}).getSum();
		return (p1.getFrequency() + p2.getFrequency()) * (2.0 - c) / 2.0;
	}

	
	private Distribution<Address> getMin() {
		Distribution<Address> d1 = getP1().getSelectDistribution().copynormalize();
		Distribution<Address> d2 = getP2().getSelectDistribution().copynormalize();

		return Distribution.convolve(d1, d2, new F2<Double, Double, Double>() {
			@Override
			public Double f(Double a, Double b) {
				return Math.min(a, b);
			}
		});
	}

	/**
	 * Compute composite select distribution
	 * 
	 * @return composite select distribution
	 */
	@Select
	public Distribution<Address> select() {
		Distribution<Address> d1 = getP1().getSelectDistribution();
		Distribution<Address> d2 = getP2().getSelectDistribution();
		if (d1.isEmpty() && d2.isEmpty()) {
			return null;
		}
		Distribution<Address> temp = d1.add(d2).subtract(getMin());
		temp.normalize();
		return temp;
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
					.printf("Broken component distribution diagnostic for x=%s: p1=%s=%s %s, p2=d2=%s %s\n",
							x, getP1().getClass(), a, (d1.containsKey(x) ? "" : "(MISSING)"), getP2().getClass(), b,
							(d2.containsKey(x) ? "" : "(MISSING)"));
			d1.dump(System.err);
			d2.dump(System.err);
			throw new RuntimeException("broken component distributions");
		}

		double m = Math.min(a, b);

		double alpha = (a - m) / (a + b - m);
		double beta = (b - m) / (a + b - m);
		double gamma = m / (a + b - m);

		double[] weights = { alpha, beta, gamma };

		switch (weightedChoice(rng, weights)) {
		case 0:
			return MergeSelectionCase.P1;
		case 1:
			return MergeSelectionCase.P2;
		case 2:
			return MergeSelectionCase.BOTH;
		default:
				throw new RuntimeException("impossible to reach exception");
		}
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
	
	public static MergeCorrelated merge(Protocol p1, Protocol p2, double p1share) {
		return new MergeCorrelated(p1, p2, p1share);
	}

	public static Protocol merge(List<Protocol> plist) {
		// build a balanced tree of merges
		int n = plist.size();			
		if(n == 1) {
			return plist.get(0);
		}
		else if(n == 2) {
			return merge(plist.get(0), plist.get(1));
		}
		else if(n % 2 == 0) {
			List<Protocol> left = Functional.<Protocol>sublist(plist, 0, n/2);
			List<Protocol> right = Functional.<Protocol>sublist(plist, n/2, n);
			return merge(merge(left), merge(right));
		}
		else { // n is odd, n >= 3
			List<Protocol> left = Functional.<Protocol>sublist(plist, 0, n/2);
			List<Protocol> right = Functional.<Protocol>sublist(plist, n/2, n); // odd length
			return merge(merge(left), merge(right), ((double)left.size()) / ((double)plist.size())); // balance the shares
		}
		
	}
	
	private static final long serialVersionUID = 1L;
}
