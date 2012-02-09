package org.princehouse.mica.example;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.runtime.Runtime;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTree;
import org.princehouse.mica.lib.TreeCountNodes;
import org.princehouse.mica.lib.TreeLabelNodes;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.TestHarness;

import fj.F3;

/**
 * Tests leader election + spanning tree + counting + labeling
 * @author lonnie
 *
 */
public class RunDemoCompositeProtocol {

	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, createNodeFunc);
	}

	/**
	 * This createNodeFunc is used by the test harness to create individual node instances.
	 * 
	 * F3 is a fancy way of creating a callable object that can be passed 
	 * as a parameter to the test harness.  The f() function is what's important.
	 * 
	 * 
	 */
	public static F3<Integer, Address, List<Address>, DemoCompositeProtocol> createNodeFunc = new F3<Integer, Address, List<Address>, DemoCompositeProtocol>() {
		@Override
		public DemoCompositeProtocol f(Integer i, Address address,
				List<Address> neighbors) {
			Overlay view = new StaticOverlay(neighbors);
			return new DemoCompositeProtocol(view, i);
		}
	};
}

