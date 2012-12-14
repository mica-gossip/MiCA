package org.princehouse.mica.base.model;

import org.princehouse.mica.util.Logging;
import org.princehouse.mica.util.WeakHashSet;
import org.princehouse.mica.util.reflection.FindReachableObjects;

public class RuntimeContextManager {
	// ---- agent execution context utilities ------------
	private ThreadLocal<WeakHashSet<Object>> foreignObjects = new ThreadLocal<WeakHashSet<Object>>();
	private ThreadLocal<RuntimeState> foreignRuntimeState = new ThreadLocal<RuntimeState>();
	private ThreadLocal<Runtime> nativeRuntime = new ThreadLocal<Runtime>();

	// for debugging, remember which piece of code last set the native runtime
	private ThreadLocal<String> debugLastSetLocation = new ThreadLocal<String>();
	
	public void clear() {
		debugLastSetLocation.remove();
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
		
		Runtime rt = getNativeRuntime();
		if(rt == null) {
			throw new RuntimeException("Cannot get runtime state: No native runtime is set");
		}
		return rt.getRuntimeState();
	}

	public void setNativeRuntime(Runtime rt) {		
		String location = Logging.getLocation(1);
		if(nativeRuntime.get() != null) {
			throw new RuntimeException(String.format("Attempt to set native runtime without clearing first. Last set at %s\n", debugLastSetLocation.get()));
		}
		debugLastSetLocation.set(location);
		nativeRuntime.set(rt);
	}

	public Runtime getNativeRuntime() {
		Runtime rt = nativeRuntime.get();
		if(rt == null) {
			throw new RuntimeException("You forgot to set the native runtime");
		}
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
