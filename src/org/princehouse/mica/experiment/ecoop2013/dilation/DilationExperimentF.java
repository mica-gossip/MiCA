package org.princehouse.mica.experiment.ecoop2013.dilation;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.example.FindMin;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;

/**
 * Compares dilation of PUSH vs PULL vs PUSHPULL for findmin on a complete graph
 * 
 * @author lonnie
 * 
 */
public class DilationExperimentF extends TestHarness implements
		ProtocolInstanceFactory {

	public static void main(String[] args) {
		for (Protocol.Direction direction : new Protocol.Direction[] {
				Protocol.Direction.PUSHPULL, Protocol.Direction.PUSH,
				Protocol.Direction.PULL }) {

			for (int i = 0; i < 3; i+=2) {
				int dilation = i * i;
				
				DilationExperimentF test = new DilationExperimentF(dilation,
						direction);
				MicaOptions options = test.parseOptions(args);
				if (i > 0 || !direction.equals(Protocol.Direction.PUSHPULL)) {
					options.clearLogdir = false;
				}
				options.expname = String.format("%s_d%s_", direction, dilation);
				test.runMain(options, test);
			}
		}
	}

	@Override
	public MicaOptions defaultOptions() {
		MicaOptions options = super.defaultOptions();
		options.n = 1000;
		options.implementation = "sim";
		options.graphType = "complete";
		options.timeout = 10000;
		// set very high to prevent high dilation from timing out and aborting
		options.roundLength = 100000;
		options.stagger = options.roundLength;
		options.stopAfter = 20;
		options.logsDisable = Functional.list(new String[] { "state", "rate",
				"select", "merge", "error" });
		return options;
	}

	
	public DilationExperimentF(int dilation, Protocol.Direction direction) {
		this.dilation = dilation;
		this.direction = direction;
	}

	public int dilation = 0;
	public Protocol.Direction direction;

	@Override
	public Protocol createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {
		Protocol p = new FindMinChatty(nodeId, overlay, direction,
				String.format("%s-dilation-%s", direction, dilation));
		if (dilation > 0) {
			p = Dilator.dilate(dilation, p);
		}
		return p;
	}

	
}
