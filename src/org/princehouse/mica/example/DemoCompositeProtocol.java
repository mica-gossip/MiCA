package org.princehouse.mica.example;

import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.TestHarness;


public class DemoCompositeProtocol {
	
	/** 
	 * See TestHarness.TestHarnessOptions for command line options 
	 * @param args
	 */
	public static void main(String[] args) {
		TestHarness.main(args, new TestHarness.ProtocolInstanceFactory<FourLayerTreeStack>() {
			@Override
			public FourLayerTreeStack createProtocolInstance(int nodeId,
					Address address, List<Address> view) {
				Overlay bootstrapView = new StaticOverlay(view);
				return new FourLayerTreeStack(bootstrapView, nodeId);
			}
		});
	}
	
}
