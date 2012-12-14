package org.princehouse.mica.base.model;

import java.io.Serializable;

public interface CommunicationPatternAgent<M1 extends Serializable, M2 extends Serializable> {

	/*
	 * The MiCA communication pattern is as follows between two nodes, a and b:
	 * 
	 * m1 = f1(a) 
	 *               ----- m1 ----> 
	 *                               m2 = f2(b,m1)               
	 *               <---- m2 ----- 
	 * f3(a,m2)
	 * 
	 * 
	 * The actual communication should be left to the runtime, but the messages
	 * m* and functions f* are defined by the implementation of this interface.
	 */

	/**
	 * Modifies a's state and generates a message for node b
	 * 
	 * @param a
	 * @return
	 */
	public abstract M1 f1(Protocol a);

	/**
	 * Modifies b's state and generates a return message for a
	 * 
	 * @param b
	 * @param m1
	 * @return
	 */
	public abstract M2 f2(Protocol b, M1 m1);

	public abstract void f3(Protocol a, M2 m2);

}
