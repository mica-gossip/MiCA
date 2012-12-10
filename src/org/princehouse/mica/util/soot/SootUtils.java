package org.princehouse.mica.util.soot;

import soot.Scene;
import soot.SootClass;

public class SootUtils {

	/** 
	 * Just like Scene.forceResolve, but operates on a Class<?> object instead of a classname string
	 * 
	 * @param klass
	 * @param level
	 * @return
	 */
	public static SootClass forceResolveJavaClass(Class<?> klass, int level) {
		String className = klass.getCanonicalName();
		return Scene.v().forceResolve(className, level);
	}
	
	
}
