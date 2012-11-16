package org.princehouse.mica.example;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

public class RoundRobinMerge extends BaseProtocol {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private Protocol p1 = null;
	private Protocol p2 = null;
	private boolean gossipP1 = true;
	
	public RoundRobinMerge(Protocol p1, Protocol p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	@View
	public Distribution<Address> view() {
		if(gossipP1) {
			return p1.getView();
		} else {
			return p2.getView();
		}
	}
	
	@Override
	public void preUpdate(Address a) {
		if(gossipP1) {
			p1.preUpdate(a);
		} else {
			p2.preUpdate(a);
		}
		
		gossipP1 = !gossipP1;
	}
	
	
	@Override 
	public void postUpdate() {
		if(!gossipP1) {
			p2.postUpdate();
		} else {
			p1.postUpdate();
		}
	}
	
	@GossipUpdate 
	public void update(RoundRobinMerge that) {
		if(!gossipP1) { // !gossipP1 because it gets flipped in preUpdate
			p1.executeUpdate(that.p1);
		} else {
			p2.executeUpdate(that.p2);
		}
	}
	
	@GossipRate
	public double rate() {
		// WARNING: rates really need to be equal for this round robin implementation to make sense
		return p1.getRate() + p2.getRate();
	}
	
}
