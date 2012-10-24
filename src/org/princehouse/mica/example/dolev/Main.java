package org.princehouse.mica.example.dolev;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness.TestHarnessOptions;

/**
 * Test the pulse protocol
 * 
 * @author lonnie
 *
 */
public class Main {
	
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		final TestHarness<Pulse> harness = new TestHarness<Pulse>();
		
		TestHarnessOptions options = TestHarness.parseOptions(args);
		options.graphType = "complete";
		
		final int d = 200; // ms
		final int f = options.n / 4;
		final int t1 = d * 5;
		final int t2 = d * 5;
		final int t3 = d * 5;
		final int t4 = d * 5;
				
		ProtocolInstanceFactory<Pulse> factory = new ProtocolInstanceFactory<Pulse>() {
			@Override
			public Pulse createProtocolInstance(int nodeId, Address address,
					Overlay overlay) {
			
						List<Address> neighborList;
						try {
							neighborList = Functional.list(overlay.getOverlay(null).keySet());
						} catch (SelectException e) {
							throw new RuntimeException(e);
						}
						
						Pulse bc = new Pulse(neighborList, d, t1, t2, t3, t4, f);
						return bc;
			}
			
		};
		harness.runMain(options, factory);
	}
}
