package org.princehouse.mica.util.reflection;

import java.util.List;

import org.princehouse.mica.util.Functional;

public abstract class FindReachableObjects<T> extends ReachableObjectVisitor {

	private List<T> result = null;
	
	/**
	 * Return a list of reachable objects for which the match method returns true.
	 * Matching objects will be cast to type T
	 * 
	 * @param root
	 * @return List of matching reachable objects
	 */
	public List<T> find(Object root) {
		result = Functional.list();
		analyze(root);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void visit(Object reachable) {
		if(match(reachable)) {
			result.add((T) reachable);
		}
	}

	public abstract boolean match(Object obj); 
}
