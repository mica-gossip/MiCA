package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

import fj.F2;
import static org.princehouse.mica.util.Randomness.weightedChoice;

/**
 * 
 * Not used
 * 
 * @author lonnie
 *
 * @param <P1>
 * @param <P2>
 */
public class MergeGeneric<P1 extends Protocol, P2 extends Protocol> extends
		BaseProtocol {

	private P1 p1;
	private P2 p2;

	public MergeGeneric(P1 p1, P2 p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public void logstate() {
		((BaseProtocol) p1).logstate();
		((BaseProtocol) p2).logstate();
	}

	@Select
	public Distribution<Address> select() {

		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();

		// sanity check
		if (d1.getSum() < 10e-7)
			throw new RuntimeException("d1 is null");
		if (d2.getSum() < 10e-7)
			throw new RuntimeException("d2 is null");

		return Distribution.convolve(d1, d2,
				new F2<Double, Double, Double>() {
					@Override
					public Double f(Double px1, Double px2) {
						return px1 + px2 + px1 * px2;
					}
				}).normalize();
	}

	@GossipUpdate
	public void update(MergeGeneric<P1,P2> other) {
		Address x = other.getAddress();
		
		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();

		double a = d1.get(x);
		double b = d2.get(x);

		if(a < 1e-5 && b < 1e-5) {
			System.err.printf("Broken component distribution diagnostic for x=%s: d1=%s %s, d2=%s %s\n", x, 
					a, (d1.containsKey(x) ? "" : "(MISSING)"), 
					b, (d2.containsKey(x) ? "" : "(MISSING)"));
			d1.dump(System.err);
			d2.dump(System.err);
			throw new RuntimeException("broken component distributions");
		}
		
		double alpha = a * (1-b) / (a+b-a*b);
		double beta = b * (1-a) / (a+b-a*b);
		double gamma = a * b / (a+b-a*b);
		
		double[] weights = {alpha, beta, gamma};
		
		switch(weightedChoice(getRuntimeState().getRandom(), weights)) {
		case 0:
			// only protocol 1 gossips
			p1.executeUpdate(other.p1);
			logCsv("merge-update,p1");
			break;
		case 1:
			// only protocol 2 gossips
			p2.executeUpdate(other.p2);
			logCsv("merge-update,p2");
			break;
		case 2:
			// both protocols gossip
			logCsv("merge-update,both");
			p1.executeUpdate(other.p1);
			p2.executeUpdate(other.p2);
		}
	}

	@Override
	public String getName() {
		return String.format("merge(%s || %s)", ((BaseProtocol) p1).getName(),
				((BaseProtocol) p2).getName());
	}

	private static final long serialVersionUID = 1L;
}
