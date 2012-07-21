package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

// FIXME needs pre, post update, etc

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

	@Select
	public Distribution<Address> select() {
		return getProtocol().getSelectDistribution();
	}
	
	@GossipRate
	public double rate() {
		return getProtocol().getFrequency();
	}
	
	@GossipUpdate
	public void update(ProtocolAdapter that) {
		this.getProtocol().executeUpdate(that.getProtocol());
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
