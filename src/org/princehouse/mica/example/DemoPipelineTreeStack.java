package org.princehouse.mica.example;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.Pipeline;
import org.princehouse.mica.lib.Pipeline.ProtocolFactory;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.RoundRobinOverlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.TestHarness;


public class DemoPipelineTreeStack {

	public static void main(String[] args) {
		TestHarness.main(args, new TestHarness.ProtocolInstanceFactory<Pipeline<FourLayerTreeStack>>() {
			@Override
			public Pipeline<FourLayerTreeStack> createProtocolInstance(int i,
					Address address, Overlay overlay) {
				final int j = i;
				// Create a static overlay to bootstrap our set of neighbors
				final Overlay view = new RoundRobinOverlay(Functional.list(overlay.getView(null).keySet()));
				return new Pipeline<FourLayerTreeStack>(5, new ProtocolFactory<FourLayerTreeStack>() {
					@Override
					public FourLayerTreeStack createProtocol() {
						return new FourLayerTreeStack(view, j);
					}
				});
			}
		});
	}

}
