package org.princehouse.mica.experiment.ecoop2013.dilation;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.example.FindMin;
import org.princehouse.mica.lib.abstractions.Overlay;

public class FindMinChatty extends FindMin<Integer> {
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