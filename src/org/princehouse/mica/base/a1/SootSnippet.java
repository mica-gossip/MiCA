package org.princehouse.mica.base.a1;


/**
 * RuntimeAgent for the simple runtime.
 * 
 * @author lonnie
 * 
 * @param 
 *            Top-level Protocol class
 */
class SootSnippet {

	@SuppressWarnings("unused")
	private void partitionUpdate() {
		// FIXME
		//    stub for soot --- add back in to SimpleRuntime framework once december re-architecting finished
		// partitions updateMethod according to locations
		// this -> local
		// param0 -> remote
		// TODO implement partitioning!

		// TODO first task: just run soot...

		/*
		String sootArgs[] = {};

		Options.v().parse(sootArgs);
		SootClass c = SootUtils.forceResolveJavaClass(
				updateMethod.getDeclaringClass(), SootClass.BODIES);
		c.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		SootMethod method = c.getMethodByName(updateMethod.getName());
		Scene.v().setEntryPoints(Functional.list(method));
		PackManager.v().runPacks();
 		*/
	}

	

	
}
