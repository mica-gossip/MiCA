package org.princehouse.mica.base.simple;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

abstract class Selector<Q extends Protocol> {
	
	public abstract Distribution<Address> select(Runtime<?> rt, Q pinstance);
}