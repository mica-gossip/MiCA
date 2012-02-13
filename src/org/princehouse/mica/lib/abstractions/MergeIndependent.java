package org.princehouse.mica.lib.abstractions;

import static org.princehouse.mica.util.Randomness.weightedChoice;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
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
public class MergeIndependent extends BaseProtocol {

	private Protocol p1;
	private Protocol p2;

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
	 * @param p2
	 */
	public void setP2(Protocol p2) {
		this.p2 = p2;
	}

	/**
	 * Constructor to create independent merged p1 + p2
	 * @param p1 First subprotocol
	 * @param p2 Second subprotocol
	 */
	public MergeIndependent(Protocol p1, Protocol p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public void logstate() {
		((BaseProtocol) p1).logstate();
		((BaseProtocol) p2).logstate();
	}

	/**
	 * Composite select distribution
	 * 
	 * @return
	 */
	@Select
	public Distribution<Address> select() {

		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();

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
		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();
		double c = d1.multiply(d2).getSum();
		// TODO not sure if this is right??? double check
		return (p1.getFrequency() + p2.getFrequency()) * (2.0 - c) / 2.0;
	}
	
	/**
	 * Composite update function
	 * 
	 * @param other
	 */
	@GossipUpdate
	public void update(MergeIndependent other) {
		Address x = other.getAddress();

		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();

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
			// only protocol 1 gossips
			p1.executeUpdate(other.p1);
			log("merge-update,p1");
			break;
		case 1:
			// only protocol 2 gossips
			p2.executeUpdate(other.p2);
			log("merge-update,p2");
			break;
		case 2:
			// both protocols gossip
			log("merge-update,both");
			p1.executeUpdate(other.p1);
			p2.executeUpdate(other.p2);
		}
	}

	@Override
	public String getName() {
		return String.format("merge(%s || %s)", ((BaseProtocol) p1).getName(),
				((BaseProtocol) p2).getName());
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
