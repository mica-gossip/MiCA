package org.princehouse.mica.util;

import java.io.Serializable;
import java.util.List;

import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.SinglyLinkedRingOverlay;
import org.princehouse.mica.util.harness.TestHarnessBaseGraph;

import fj.F;

public class SinglyLinkedRingGraph extends TestHarnessBaseGraph {

	public SinglyLinkedRingGraph(List<Address> addresses) {
		super(addresses);
	}

	@Override
	public F<Integer, List<Integer>> getNeighbors() {
		final int n = size();
		return new F<Integer,List<Integer>>() {
			@Override
			public List<Integer> f(Integer i) {
				return Functional.list((i+1) % n);
			}
		};
	}

	@Override
	public Overlay getOverlay(Address addr) {
		Address succ = getNeighbors(addr).get(0);
		return new StaticSinglyLinkedRingOverlay(succ);
	}
	
	
	public static class StaticSinglyLinkedRingOverlay implements SinglyLinkedRingOverlay, Serializable {
		private static final long serialVersionUID = 1L;
		public StaticSinglyLinkedRingOverlay(Address succ) {
			this.succ = succ;
		}
		private Address succ;
		@Override
		public Distribution<Address> getOverlay(RuntimeState rts) {
			return Distribution.singleton(getSuccessor());
		}
		@Override
		public Address getSuccessor() {
			return succ;
		}
	}

}
