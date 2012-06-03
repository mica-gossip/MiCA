package org.princehouse.mica.example;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;


public class RunCompositeProtocol {
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, new TestHarness.ProtocolInstanceFactory<FourLayerTreeStack>() {
			@Override
			public FourLayerTreeStack createProtocolInstance(int nodeId,
					Address address, Overlay overlay) {
				return new FourLayerTreeStack(overlay, nodeId);
			}
		});
	}
	
}
