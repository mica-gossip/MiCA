package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.View;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

/**
 * ProtocolAdapter wraps another protocol, passing rate/select/update directly through
 * 
 * It serves as a base for operations that modify other protocols' existing behavior.
 * 
 * @author lonnie
 *
 */
public class ProtocolAdapter extends BaseProtocol {
	private static final long serialVersionUID = 1L;
	
	private BaseProtocol protocol;
	
	public ProtocolAdapter(BaseProtocol protocol) {
		setProtocol(protocol);
	}
		
	public BaseProtocol getProtocol() {
		return protocol;
	}


	public void setProtocol(BaseProtocol protocol) {
		this.protocol = protocol;
	}

	@View
	public Distribution<Address> select() {
		return getProtocol().getView();
	}
	
	@GossipRate
	public double rate() {
		return getProtocol().getRate();
	}
	
	@GossipUpdate
	@Override
	public void update(Protocol p) {
		ProtocolAdapter that = (ProtocolAdapter) p;
		this.getProtocol().update(that.getProtocol());
	}
	
	@Override
	public void preUpdate(Address selected) {
		getProtocol().preUpdate(selected);
	}
	
	@Override
	public void postUpdate() {
		getProtocol().postUpdate();
	}

}
