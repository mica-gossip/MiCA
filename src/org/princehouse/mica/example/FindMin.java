package org.princehouse.mica.example;


import java.util.Comparator;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.Overlay;

/**
 * 
 * 
 * @author lonnie
 *
 */
public abstract class FindMin<T> extends BaseProtocol implements Comparator<T> {

	private static final long serialVersionUID = 1L;

	private T value = null;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public final Protocol.Direction direction;

	@View
	public Overlay overlay = null;
	
	public FindMin(T initialValue, Overlay overlay, Protocol.Direction direction) {
		this.direction = direction;
		setValue(initialValue);
		this.overlay = overlay;
	}
	
	@GossipUpdate
	@Override
	public void update(Protocol other) {
		@SuppressWarnings("unchecked")
		FindMin<T> that = (FindMin<T>) other;
		
		int comparison = compare(this.getValue(),that.getValue());
		
		if(comparison < 0) {
			// local value smaller than remote
			if(direction.push()) {
				that.setValue(this.getValue());
			}
		} else if(comparison > 0) {
			// remote value smaller than local
			if(direction.pull()) {
				this.setValue(that.getValue());
			}
		}
	}
	
}
