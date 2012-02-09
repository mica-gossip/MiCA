package org.princehouse.mica.base.runtime;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

public abstract class Selector<Q extends Protocol> {
	
	public abstract Distribution<Address> select(Runtime<?> rt, Q pinstance);
}