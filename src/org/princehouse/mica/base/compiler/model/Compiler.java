package org.princehouse.mica.base.compiler.model;

import org.princehouse.mica.base.Protocol;

public abstract class Compiler {

	public Compiler() {}
		
	public abstract <T extends Protocol> RuntimeAgent<T> compile(T pinstance);
}
