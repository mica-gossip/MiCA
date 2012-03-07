package org.princehouse.mica.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class Randomness {
	
	private static final double EPSILON = 10e-7;
	
	public static class EmptyCollection extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
	
	public static Random random = new Random();
	
	public static void seedRandomness(int rseed) {
		random = new Random(rseed);
	}
	
	public static <T> T choose(Collection<T> c) {
		if(c.size() == 0)
			throw new EmptyCollection();
		int choice = random.nextInt(c.size());
		int i = 0;
		for(T obj : c) {
			if(i++ == choice)
				return obj;
		}
		throw new RuntimeException("should not be reachable");
	}
	
	public static <T> T choose(Iterable<T> c) {
		LinkedList<T> l = new LinkedList<T>();
		for(T x : c) l.add(x);
		return choose(l);
	}
	
	public static <T> T choose(T[] array) {
		if(array.length == 0)
			throw new EmptyCollection();
		return array[random.nextInt(array.length)];
	}
	
	
	/**
	 * Choose an index from an array using the supplied weights
	 * 
	 * @param r
	 * @param weights  Must sum to 1.0! This is currently checked at runime.
	 */
	public static int weightedChoice(Random r, double[] weights) {
		
		// validate input.  remove this if performance is a bottleneck
		double t = 0;
		for(double w : weights) {
			if(w < -EPSILON) throw new RuntimeException(String.format("subzero weight %f",w));
			if(Double.isNaN(w)) {
				throw new RuntimeException("NaN weight!");
			}
			t += w;
		}
		if(Math.abs(t-1.0) >= EPSILON) {
			if(weights.length == 3) {
				System.out.printf("WEIGHTED CHOICE:   w = ( %f, %f, %f )\n",weights[0],weights[1],weights[2]);
			}
			throw new RuntimeException("weights do not sum to one");
		}
		
		// make a weighted choice
		double x = r.nextDouble();
		
		if(x > 1.0) throw new RuntimeException("WTF");
		
		//if(weights.length == 3) {
		//	System.out.printf("WEIGHTED CHOICE: x=%f    w = ( %f, %f, %f )\n",x,weights[0],weights[1],weights[2]);
		//}
		
		for(int i = 0; i < weights.length; i++) {
			if(x <= weights[i])
				return i; 
			else
				x -= weights[i];
		}
		
		throw new RuntimeException("program should never reach this point");
	}
}

