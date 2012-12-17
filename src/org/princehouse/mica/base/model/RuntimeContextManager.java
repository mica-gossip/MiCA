package org.princehouse.mica.base.model;

import java.util.WeakHashMap;

import org.princehouse.mica.util.Logging;
import org.princehouse.mica.util.WeakHashSet;
import org.princehouse.mica.util.reflection.FindReachableObjects;

public class RuntimeContextManager {
	// ---- agent execution context utilities ------------
	private ThreadLocal<WeakHashSet<Object>> foreignObjects = new ThreadLocal<WeakHashSet<Object>>();
	private ThreadLocal<RuntimeState> foreignRuntimeState = new ThreadLocal<RuntimeState>();
	private ThreadLocal<MicaRuntime> nativeRuntime = new ThreadLocal<MicaRuntime>();

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

		MicaRuntime rt = getNativeRuntime();
		if (rt == null) {
			throw new RuntimeException(
					"Cannot get runtime state: No native runtime is set");
		}
		return rt.getRuntimeState();
	}

	public void setNativeRuntime(MicaRuntime rt) {
		String location = Logging.getLocation(1);
		if (nativeRuntime.get() != null) {
			throw new RuntimeException(
					String.format(
							"Attempt to set native runtime without clearing first. Last set at %s\n",
							debugLastSetLocation.get()));
		}
		debugLastSetLocation.set(location);
		nativeRuntime.set(rt);
	}

	public MicaRuntime getNativeRuntime() {
		MicaRuntime rt = nativeRuntime.get();
		if (rt == null) {
			throw new RuntimeException("You forgot to set the native runtime");
		}
		return rt;
	}

	private WeakHashMap<Protocol, WeakHashSet<Object>> foreignObjectCache = new WeakHashMap<Protocol, WeakHashSet<Object>>();

	public void setForeignRuntimeState(Protocol rootProtocol, RuntimeState rts) {
		assert (foreignObjects.get() == null);
		assert (foreignRuntimeState.get() == null);
		foreignObjects.set(getForeignObjects(rootProtocol));
		foreignRuntimeState.set(rts);
	}

	// caching refelection results can be dangerous; it will cause incorrect execution if any protocol is created dynamically after
	// its parent has already been analyzed
	private WeakHashSet<Object> getForeignObjects(Protocol rootProtocol) {
		if (MiCA.getOptions().reflectionCache) {
			WeakHashSet<Object> temp = foreignObjectCache.get(rootProtocol);
			if (temp != null) {
				return temp;
			}
		}

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
		
		if(MiCA.getOptions().reflectionCache) {
			foreignObjectCache.put(rootProtocol, whs);
		}
		
		return whs;
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
