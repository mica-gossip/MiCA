package org.princehouse.mica.experiment.dilation;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.MiCA;
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
 * Test of dilation of findmin protocol on a complete graph
 * 
 * @author lonnie
 * 
 */
public class DilationExperimentA extends TestHarness implements
		ProtocolInstanceFactory {
	
	public static void main(String[] args) {
		for(int dilation = 0 ; dilation < 5; dilation++) {
			DilationExperimentA test = new DilationExperimentA(dilation);
			MicaOptions options = test.parseOptions(args);
			if(dilation > 0) {
				options.clearLogdir = false;
			}
			options.expname="abcdefghijklmnop".substring(dilation,dilation+1);
			test.runMain(options,test);
		}
	}
	
	@Override
	public MicaOptions defaultOptions() {
		MicaOptions options = super.defaultOptions();
		options.n = 25;
		options.implementation = "simple";
		options.graphType = "complete";
		options.timeout = 2000;
		// set very high to prevent high dilation from timing out and aborting
		options.roundLength = 5000;
		options.stagger = options.roundLength;
		options.stopAfter = 15;
		options.logsDisable = Functional.list(new String[]{
				"state","rate","select","merge","error"
		});
		return options;
	}

	public DilationExperimentA(int dilation) {
		this.dilation = dilation;
		direction = Protocol.Direction.PUSHPULL;
	}
	
	public int dilation = 0;
	public Protocol.Direction direction;
	
	@Override
	public Protocol createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {
		String expname = MiCA.getOptions().expname;
		Protocol p = new FindMinChatty(nodeId, overlay, direction, String.format("%s-dilation-%s",expname,dilation));
		if(dilation > 0) {
			p = Dilator.dilate(dilation, p);
		}
		return p;
	}

	public static class FindMinChatty extends FindMin<Integer> {
		private static final long serialVersionUID = 1L;
		private String name = null; // used for logging
		
		public String getName() {
			return name;
		}
		
		public void setName(String n) {
			this.name = n;
		}
		
		public FindMinChatty(Integer initialValue, Overlay overlay,
				Direction direction, String name) {
			super(initialValue, overlay, direction);
			setName(name);
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			return o1.compareTo(o2);
		}
		
		@Override
		public void setValue(Integer value) {
			super.setValue(value);
			if(getName() != null) 
				logJson(LogFlag.user, "notable-event-change", getName());
		}
	
		@GossipUpdate
		@Override
		public void update(Protocol other) {
			logJson(LogFlag.user, "notable-event-gossip", getName());
			super.update(other);
		}
	}
}
