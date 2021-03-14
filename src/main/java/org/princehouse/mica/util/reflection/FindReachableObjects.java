package org.princehouse.mica.util.reflection;

/**
 * When analyze(root) is called, every object that matches match(obj) will be
 * passed to the add() function, casting the object to the specificied type T.
 * 
 * @author lonnie
 * 
 * @param <T>
 */
public abstract class FindReachableObjects<T> extends ReachableObjectVisitor {

    @SuppressWarnings("unchecked")
    @Override
    public void visit(Object reachable) {
        if (match(reachable)) {
            add((T) reachable);
        }
    }

    public abstract void add(T obj);

    public abstract boolean match(Object obj);
}
