package org.princehouse.mica.util;

import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.HashMap;
import fj.data.List;
import fj.data.Option;

/**
 * FunctionalJava utility methods
 * @author lonnie
 *
 */
public class FJ {
	
	// first class function for list append
	// XXX (should be built into fj?)
	public static <T> F2<List<T>,List<T>,List<T>> append() {
		return new F2<List<T>,List<T>,List<T>>() {
			@Override
			public List<T> f(List<T> arg0, List<T> arg1) {
				return arg0.append(arg1);
			}
		};
	}
	
	
	/**
	 * Return a list of the key,value pairs in the given hashmap
	 * XXX (should be built into fj?)
	 * 
	 * @param <A>
	 * @param <B>
	 * @param h
	 * @return
	 */
	public static <A,B> List<P2<A,B>> pairs(final HashMap<A,B> h) {
		return h.keys().map(new F<A,P2<A,B>>() {
			@Override
			public P2<A, B> f(A key) {
				return P.p(key,h.get(key).some());
			}
		});
	}
	
	public static <T> List<T> listFromArray(T[] ar) {
		List<T> l = List.nil();
		for(T x : ar) l = l.snoc(x);
		return l;
	}
	
	public static <T> boolean contains(List<T> l, F<T,Boolean> f) {
		Option<T> o = l.find(f);
		return o.isSome();
	}
}
