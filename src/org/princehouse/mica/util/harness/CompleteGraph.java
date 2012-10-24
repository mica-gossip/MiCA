package org.princehouse.mica.util.harness;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public class CompleteGraph extends TestHarnessBaseGraph {
	
	public CompleteGraph(List<Address> addresses) {
		super(addresses);
	}
	
	public CompleteGraph(int n, F<Integer,Address> addressFunc) {
		super(n,addressFunc);
	}
	
	public F<Integer,List<Integer>> getNeighbors() {
		final int n = size();
		return new F<Integer,List<Integer>>() {
			@Override
			public List<Integer> f(Integer i) {
				List<Integer> l = Functional.list();
				for(int j = 0; j < n; j++) {
					if(i == j) continue;
					l.add(j);
				}
				return l;
			}
		};
	}
}
