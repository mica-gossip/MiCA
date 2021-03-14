package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.model.Protocol;

public interface BinaryCompositeProtocol extends CompositeProtocol {
	public void setP1(Protocol p);
	public void setP2(Protocol p);
	public Protocol getP1();
	public Protocol getP2();
}
