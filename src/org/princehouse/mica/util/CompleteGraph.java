package org.princehouse.mica.util;

import java.util.List;

import fj.F;

public class CompleteGraph implements TestHarnessGraph {
	private int n;
	
	// only works on even degrees for the time being
	public CompleteGraph(int n) {
		this.n = n;
	}
	
	public F<Integer,List<Integer>> getNeighbors() {
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
