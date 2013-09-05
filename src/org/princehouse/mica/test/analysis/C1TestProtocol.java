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

	public Integer unusedField = 12;
	
	public int getUnusedField() {
		return unusedField;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void update(Protocol temp) {
		C1TestProtocol that = (C1TestProtocol) temp;
		int quz = 101;
		this.x = that.x * 2;
		that.x += 1;
		Object rslt = this.f1(that.x);
		if (that.y > 1000) {
			quz = 102;
			that.f1(rslt);
			this.x = 1;
		} else {
			quz = 103;
			q[3] = 7;
			this.x = 2;
			while(that.f1(this.q[7]) != null) {
				that.q[1] = this.q[3];				
			}
		}
		this.x = quz; // should create a points-to set with two entries
		z.z = q[7];
		
		while(quz-- > 0) { // loop does not terminate
			this.y += q[quz % 5];
		}
		
	}

	public Object f1(Object inp) {
		return new Object();
	}

}
