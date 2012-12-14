package org.princehouse.mica.base.model;

import org.princehouse.mica.util.WeakHashSet;
import org.princehouse.mica.util.reflection.FindReachableObjects;

public class RuntimeContextManager {
	// ---- agent execution context utilities ------------
	private ThreadLocal<WeakHashSet<Object>> foreignObjects = null;
	private ThreadLocal<RuntimeState> foreignRuntimeState = null;
	private ThreadLocal<Runtime> nativeRuntime = null;


	public void clear() {
		foreignObjects.remove();
		foreignRuntimeState.remove();
		nativeRuntime.remove();
	}

	public RuntimeState getRuntimeState(Object o) {
		WeakHashSet<Object> foreign = foreignObjects.get();
		if (foreign != null) {
			if (foreign.contains(o)) {
				RuntimeState frts = foreignRuntimeState.get();
				assert (frts != null);
				return frts;
			}
		}
		return getNativeRuntime().getRuntimeState();
	}

	public void setNativeRuntime(Runtime rt) {
		assert (nativeRuntime.get() == null);
		nativeRuntime.set(rt);
	}

	public Runtime getNativeRuntime() {
		Runtime rt = nativeRuntime.get();
		assert (rt != null);
		return rt;
	}

	public void setForeignRuntimeState(Protocol rootProtocol, RuntimeState rts) {
		assert(foreignObjects.get() == null);
		assert(foreignRuntimeState.get() == null);

		final WeakHashSet<Object> whs = new WeakHashSet<Object>();

		FindReachableObjects<Object> reachableObjectFinder = new FindReachableObjects<Object>() {
			@Override
			public boolean match(Object obj) {
				return isRichObject(obj);
			}

			@Override
			public void add(Object obj) {
				whs.add(obj);
			}
			
		};
		reachableObjectFinder.analyze(rootProtocol);
		
		foreignObjects.set(whs);
		foreignRuntimeState.set(rts);
	}

	/**
	 * A "rich object" is one for which MiCA associates a RuntimeState. By
	 * default, only Protocol instances. Different partitioning schemes can
	 * override this method to support other kinds of rich objects.
	 * 
	 * @param o
	 * @return
	 */
	public boolean isRichObject(Object o) {
		return (o instanceof Protocol);
	}

}
