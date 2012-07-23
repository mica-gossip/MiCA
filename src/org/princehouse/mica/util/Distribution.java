package org.princehouse.mica.util;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import fj.F;
import fj.F2;

public class Distribution<T> extends HashMap<T,Double> {

	public static final double MAGNITUDE_EPS = 10e-7;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3336765334895508699L;

	public Distribution() {
		super();
	}


	@Override
	/**
	 * Unlike standard map get, this returns 0 for items that are not "in" the distribution
	 */
	public Double get(Object obj) {
		if(containsKey(obj))
			return super.get(obj);
		else
			return 0.0;
	}

	public double magnitude() {
		double s = 0.0;
		for(T obj : keySet()) {
			s += get(obj);
		}
		return s;
	}

	/**
	 * In-place normalization
	 * 
	 * @return
	 */
	public Distribution<T> ipnormalize() {
		return ipscale(1.0 / magnitude());
	}

	/**
	 * Return a scaled copy of dist 
	 * 
	 * @param d
	 * @return
	 */
	public Distribution<T> scale(double d) {
		return copy().ipscale(d);
	}
	
	/**
	 * In-place scaling
	 * 
	 * @param d
	 * @return
	 */
	public Distribution<T> ipscale(double d) {
		for(T k : keySet()) {
			put(k,get(k)*d);
		}
		return this;
	}
	
	/**
	 * In-place "bump"
	 * 
	 * Add delta to the value associated with the given key; adjust other values to keep the distribution normalized.
	 * @param key
	 * @param delta
	 * @return
	 */
	public Distribution<T> bump(T key, double delta) {
		// d' is the amount we adjust the key's value X pre-normalization in order to achieve X+delta after normalization
		double x = get(key);
		double dprime = delta / (1.0 - x - delta);
		put(key, x+dprime);
		ipnormalize();
		assert(Math.abs(get(key) - (x + delta)) < 1e-7); // sanity check
		return this;
	}

	
	
	@Override
	/**
	 * returns only keys whose probability is nonzero
	 */
	public Set<T> keySet() {
		return Functional.set(Functional.filter(super.keySet(), new F<T,Boolean>() {
			@Override
			public Boolean f(T k) {
				return get(k) > 0;
			}
		}));
	}
	
	public static <T> Distribution<T> create() {
		return new Distribution<T>();
	}
	
	public static <T> Distribution<T> create(T obj) {
		Distribution<T> pmd = create();
		pmd.put(obj, 1.0);
		return pmd;
	}
	
	public static <T> Distribution<T> convolve(Distribution<T> d1, Distribution<T> d2, F2<Double,Double,Double> f) {
		Distribution<T> pmd = create();
		for(T x : Functional.union(d1.keySet(), d2.keySet()))
			pmd.put(x, f.f(d1.get(x), d2.get(x)));
		return pmd;
	}
	
	public static <T> Distribution<T> max(Distribution<T> d1, Distribution<T> d2) {
		return Distribution.convolve(d1, d2, Functional.max);
	}

	public static <T> Distribution<T> min(Distribution<T> d1, Distribution<T> d2) {
		return Distribution.convolve(d1, d2, Functional.min);
	}

	
	public static <T> Distribution<T> uniform(Collection<T> c) {
		Distribution<T> pmd = create();
		double n = 1.0 / (double) c.size();
		for(T obj : c) {
			pmd.put(obj, n);
		}
		return pmd;
	}
	
	/**
	 * Creates a new PMD.  Not in-place
	 * 
	 * @param other
	 * @return
	 */
	public Distribution<T> add(Distribution<T> other) {
		return convolve(this,other, new F2<Double,Double,Double>() {
			@Override
			public Double f(Double a, Double b) {
				return a+b;
			}
		});
	}
	
	/**
	 * Creates a new PMD.  Not in-place
	 * 
	 * @param other
	 * @return
	 */
	public Distribution<T> subtract(Distribution<T> other) {
		return convolve(this,other, new F2<Double,Double,Double>() {
			@Override
			public Double f(Double a, Double b) {
				return a-b;
			}
		});
	}
	
	/**
	 * Creates a new PMD.  Not in-place
	 * 
	 * @param other
	 * @return
	 */
	public Distribution<T> multiply(Distribution<T> other) {
		return convolve(this,other, new F2<Double,Double,Double>() {
			@Override
			public Double f(Double a, Double b) {
				return a*b;
			}
		});
	}
	
	public T sample(Random rng) {
		double sample = rng.nextDouble();
		if(size() <= 0) {
			return null;
		}
		
		for(T x : keySet()) {
			double y = get(x);
			if(sample <= y) {
				return x;
			} else { 
				sample -= y;
			}
		}
		throw new RuntimeException("code should never reach this point");	
	}
	
	public T sample(long rseed) {
		return sample(new Random(rseed));
	}
	
	public T sample() {
		return sample(new Random().nextLong());
	}
	
	// debugging
	public void dump(PrintStream out) {
		out.printf("%d keys:\n",keySet().size());
		for(T k : keySet()) {
			out.printf("%-30s %f\n", k, get(k));
		}
		out.printf("  sum: %f\n",magnitude());
	}
	
	public boolean isEmpty() {
		return magnitude() <= MAGNITUDE_EPS;
	}

	public Distribution<T> copy() {
		Distribution<T> tmp = new Distribution<T>();
		for(T k : keySet()) {
			tmp.put(k,get(k));
		}
		return tmp;
	}

	public Distribution<T> copynormalize() {
		return copy().ipnormalize();
	}
	
	/**
	 * Create a singleton distribution
	 * (That is, dist[key] == 1,  dist[x] = 0 for x != key)
	 * @param key
	 * @return
	 */
	public static <T> Distribution<T> singleton(T key) {
		Distribution<T> dist = new Distribution<T>();
		dist.put(key, 1.0);
		return dist;
	}

	/** 
	 * Returns true if this distribution sums to one, within MAGNITUDE_EPS tolerance
	 * @return
	 */
	public boolean isOne() {
		double magnitude = magnitude();
		return Math.abs(1.0 - magnitude) <= MAGNITUDE_EPS;
	}
	
}
