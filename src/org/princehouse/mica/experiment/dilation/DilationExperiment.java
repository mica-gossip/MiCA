package org.princehouse.mica.experiment.dilation;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.example.FindMin;
import org.princehouse.mica.lib.abstractions.MergeIndependent;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;
import org.princehouse.mica.util.harness.TestHarness.ProtocolInstanceFactory;

/**
 * Tests leader election + spanning tree
 * 
 * @author lonnie
 * 
 */
public abstract class DilationExperiment extends TestHarness<Protocol> implements
		ProtocolInstanceFactory<Protocol> {

	/**
	 * should set:
	 *    direction
	 */
	public abstract void setExperimentOptions();
	
	
	public DilationExperiment() {
		setExperimentOptions();
	}
	
	public Protocol.Direction direction;
	
	@Override
	public Protocol createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {
		
		
		Protocol p1 = new FindMinChatty(nodeId, overlay, direction, "dilation-1");
		Protocol p2 = Dilator.dilate(3,new FindMinChatty(nodeId, overlay, direction, "dilation-4"));	
		return MergeIndependent.merge(p1,p2);
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
