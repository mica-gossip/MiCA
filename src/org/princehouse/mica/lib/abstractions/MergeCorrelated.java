package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.annotations.GossipFrequency;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

import fj.F2;
import static org.princehouse.mica.util.Randomness.weightedChoice;

public class MergeCorrelated extends BaseProtocol {

	private Protocol p1;
	public Protocol getP1() {
		return p1;
	}

	public void setP1(Protocol p1) {
		this.p1 = p1;
	}

	public Protocol getP2() {
		return p2;
	}

	public void setP2(Protocol p2) {
		this.p2 = p2;
	}

	private Protocol p2;

	public MergeCorrelated(Protocol p1, Protocol p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public MergeCorrelated() {
		p1 = null; p2 = null;
	}

	@GossipFrequency
	public double mergedFrequency() {
		Distribution<Address> d1 = p1.getSelectDistribution().copynormalize();
		Distribution<Address> d2 = p2.getSelectDistribution().copynormalize();
		double c = 	Distribution.convolve(d1, d2, new F2<Double,Double,Double>() {
			@Override
			public Double f(Double a, Double b) {
				return Math.min(a, b);
			}
		}).getSum();
		return (p1.getFrequency() + p2.getFrequency()) * (2.0 - c) / 2.0;
	}
	
	@Override
	public void logstate() {
		((BaseProtocol) p1).logstate();
		((BaseProtocol) p2).logstate();
	}

	public Distribution<Address> getMin() {
		Distribution<Address> d1 = p1.getSelectDistribution().copynormalize();
		Distribution<Address> d2 = p2.getSelectDistribution().copynormalize();

		return Distribution.convolve(d1, d2, new F2<Double,Double,Double>() {
			@Override
			public Double f(Double a, Double b) {
				return Math.min(a, b);
			}
		});
	}

	@Select
	public Distribution<Address> select() {
		Distribution<Address> d1 = p1.getSelectDistribution();
		Distribution<Address> d2 = p2.getSelectDistribution();
		if(d1.isEmpty() && d2.isEmpty()) {
			return null;
		}
		Distribution<Address> temp = d1.add(d2).subtract(getMin());
		temp.normalize();
		return temp;
	}

	@GossipUpdate
	public void update(MergeCorrelated other) {
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

		double m = Math.min(a,b);
		
		double alpha = (a-m) / (a+b-m);
		double beta = (b-m) / (a+b-m);
		double gamma = m / (a+b-m);

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
		return String.format("mergeC(%s || %s)", ((BaseProtocol) p1).getName(),
				((BaseProtocol) p2).getName());
	}

	public static MergeCorrelated merge(Protocol p1, Protocol p2) {
		return new MergeCorrelated(p1,p2);
	}

	private static final long serialVersionUID = 1L;
}
