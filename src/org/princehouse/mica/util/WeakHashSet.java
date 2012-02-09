package org.princehouse.mica.util;

import java.util.WeakHashMap;

public class WeakHashSet<T> {
	private WeakHashMap<T,Boolean> contents = new WeakHashMap<T,Boolean>();

	public void add(T ob) {
		contents.put(ob, true);
	}

	public boolean contains(T ob) {
		return contents.containsKey(ob);
	}
}
