package org.princehouse.mica.test.analysis;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;

public class C1TestProtocol extends BaseProtocol {

	public static class Inner {
		public int z = 5;
	}

	public Integer x = 3;
	public Integer y = 3;
	public Inner z = new Inner();
	public Integer[] q = new Integer[10];

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void update(Protocol temp) {
		C1TestProtocol that = (C1TestProtocol) temp;
		this.x = that.x * 2;
		that.x += 1;
		Object rslt = this.f1(that.x);
		if (that.y > 1000) {
			that.f1(rslt);
		} else {
			q[3] = 7;
			while(that.f1(this.q[7]) != null) {
				that.q[1] = this.q[3];				
			}
		}
		z.z = q[7];
	}

	public Object f1(Object inp) {
		return new Object();
	}

}
