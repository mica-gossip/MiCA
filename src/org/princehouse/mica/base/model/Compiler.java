package org.princehouse.mica.base.model;



/**
 * Generates RuntimeAgent<T> for a given protocol instance T.  This is called 
 * before EVERY gossip round; a compiler that does not use dynamic information
 * may wish to cache the resulting agent after executing compile() once for a given
 * Protocol class T.
 *  
 * @author lonnie
 *
 */
public abstract class Compiler {

	/**
	 * Initialize compiler
	 */
	public Compiler() {}

	/**
	 * 
	 * Generate a RuntimeAgent for the given protocol instance.
	 * 
	 * @param pinstance Protocol instance
	 * @return A RuntimeAgent. Static analysis-based agents may be reused for any 
	 * protocol instance of the given class.
	 */
	public abstract <T extends Protocol> RuntimeAgent<T> compile(T pinstance);
}
