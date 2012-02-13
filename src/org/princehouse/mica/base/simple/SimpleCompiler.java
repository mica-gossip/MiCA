package org.princehouse.mica.base.simple;


import java.util.HashMap;

import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.CompilerException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeAgent;

/**
 * The SimpleCompiler and SimpleRuntime don't do any fancy analysis.  They serialize the complete state 
 * of the iniating node and send it to the receiver. The receiver computes update() and sends the initiator's
 * new state back to it.
 * 
 * @author lonnie
 *
 */
class SimpleCompiler extends Compiler {

	private HashMap<Class<?>, SimpleRuntimeAgent<?>> agentCache = new HashMap<Class<?>,SimpleRuntimeAgent<?>>();

	/**
	 * Not much going on here; for SimpleCompiler, the SimpleAgent does all the work.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <P extends Protocol> RuntimeAgent<P> compile(P pinstance) {	
		Class<P> pclass = (Class<P>) pinstance.getClass();
		if(agentCache.containsKey(pclass))
			return (SimpleRuntimeAgent<P>) agentCache.get(pclass);
		else {
			SimpleRuntimeAgent<P> agent;
			try {
				agent = new SimpleRuntimeAgent<P>(pclass);
			} catch (CompilerException e) {
				// TODO do this error handling right
				throw new RuntimeException(e);
			}
			agentCache.put(pclass, agent);
			return agent;
		}
	}

}
