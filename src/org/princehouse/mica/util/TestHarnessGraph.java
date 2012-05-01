package org.princehouse.mica.util;

import java.util.List;

import fj.F;

public interface TestHarnessGraph {
	public F<Integer,List<Integer>> getNeighbors();
}
