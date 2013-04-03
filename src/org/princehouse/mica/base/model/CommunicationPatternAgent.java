package org.princehouse.mica.base.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaException;

public interface CommunicationPatternAgent {

	/*
	 * The MiCA communication pattern is as follows between two nodes, a and b:
	 * 
	 * m1 = f1(a) ----- m1 ----> m2 = f2(b,m1) <---- m2 ----- f3(a,m2)
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
	public abstract Serializable f1(MicaRuntime initiatorRuntime)
			throws MicaException;

	/**
	 * Modifies b's state and generates a return message for a
	 * 
	 * @param b
	 * @param m1
	 * @return
	 */
	public abstract Serializable f2(MicaRuntime receiverRuntime, Serializable m1)
			throws FatalErrorHalt, AbortRound;

	public abstract void f3(MicaRuntime initiatorRuntime, Serializable m2)
			throws FatalErrorHalt, AbortRound;

	public abstract void sendObject(Serializable obj, OutputStream out) throws FatalErrorHalt, AbortRound;
	
	public abstract Serializable recvObject(InputStream in) throws FatalErrorHalt, AbortRound;
}
