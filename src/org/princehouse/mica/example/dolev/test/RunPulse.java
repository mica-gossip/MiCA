package org.princehouse.mica.example.dolev.test;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.example.dolev.Pulse;
import org.princehouse.mica.example.dolev.PulseState;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.RoundRobinOverlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness.TestHarnessOptions;

/**
 * Test the pulse protocol
 * 
 * @author lonnie
 *
 */
public class RunPulse {
	
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		final TestHarness<Pulse> harness = new TestHarness<Pulse>();
		
		final TestHarnessOptions options = harness.parseOptions(args);
		options.graphType = "complete";
		final int f = options.n / 4;

		Randomness.seedRandomness(options.seed);
		
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
						
						List<Address> neighborListShuffled = Randomness.shuffle(Randomness.random, neighborList);
						
						Overlay rr = new RoundRobinOverlay(neighborListShuffled);
						int n = neighborList.size();						
						PulseState initialState = Randomness.choose(PulseState.class);
						
						int d = options.roundLength * (options.n + 1);
						int t4 = 20 * d;
						Pulse bc = new Pulse(rr, n, f, t4, d, initialState);
						return bc;
			}
			
		};
		harness.runMain(options, factory);
	}
}
