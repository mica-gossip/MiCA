package org.princehouse.mica.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class MarkingObjectInputStream extends ObjectInputStream {

	private WeakHashSet<Object> foreignObjectSet = new WeakHashSet<Object>();
	
	public WeakHashSet<Object> getForeignObjectSet() {
		return foreignObjectSet;
	}

	public MarkingObjectInputStream(InputStream in) throws IOException {
		super(in);
	}
	
}
