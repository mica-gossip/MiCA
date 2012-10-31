package org.princehouse.mica.example.dolev.test;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness.TestHarnessOptions;

import com.sun.corba.se.impl.javax.rmi.CORBA.Util;

/**
 * Test the pulse protocol
 * 
 * @author lonnie
 *
 */
public class RunTestMain {
	
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		final TestHarness<TestStateMachine> harness = new TestHarness<TestStateMachine>();
		
		TestHarnessOptions options = TestHarness.parseOptions(args);
		options.graphType = "complete";
		
		final int f = options.n / 4;
				
		ProtocolInstanceFactory<TestStateMachine> factory = new ProtocolInstanceFactory<TestStateMachine>() {
			@Override
			public TestStateMachine createProtocolInstance(int nodeId, Address address,
					Overlay overlay) {
			
						List<Address> neighborList;
						try {
							neighborList = Functional.list(overlay.getOverlay(null).keySet());
						} catch (SelectException e) {
							throw new RuntimeException(e);
						}
						
						List<Address> neighborListShuffled = Randomness.shuffle(Randomness.random, neighborList);
						
						TestStateMachine bc = new TestStateMachine(neighborListShuffled, f);
						return bc;
			}
			
		};
		harness.runMain(options, factory);
	}
}
