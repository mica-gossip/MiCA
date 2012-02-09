package org.princehouse.mica.base.compiler.impl;


import java.util.HashMap;

import org.princehouse.mica.base.Protocol;
import org.princehouse.mica.base.compiler.model.Compiler;
import org.princehouse.mica.base.compiler.model.RuntimeAgent;


public class SimpleCompiler extends Compiler {

	private HashMap<Class<?>, SimpleRuntimeAgent<?>> agentCache = new HashMap<Class<?>,SimpleRuntimeAgent<?>>();

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
			} catch (CPGCompileException e) {
				// TODO do this error handling right
				throw new RuntimeException(e);
			}
			agentCache.put(pclass, agent);
			return agent;
		}
	}

}
