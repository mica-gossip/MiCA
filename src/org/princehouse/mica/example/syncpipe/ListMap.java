package org.princehouse.mica.example.syncpipe;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.princehouse.mica.util.Functional;

/**
 * A ListMap behaves like a two-dimensional table, with row indices Key, column
 * indices of type Integer, and data objects of type T
 * 
 * It is logically a list, where each column is an object in the list. Columns
 * can be enqueued and dequeued. It is also logically a map
 * <Key,List<Map<Key,T>>>, where rows can be extracted by querying the key
 * 
 * @author lonnie
 * 
 * @param <Key>
 * @param <T>
 */
public class ListMap<Key, T> extends LinkedList<ListMapColumn<Key, T>> {
	
	private static final long serialVersionUID = 1L;

	public T get(Key key, Integer i) {
		return getColumn(i).get(key);
	}

	public T set(Key key, Integer i, T value) {
		return getColumn(i).put(key, value);
	}

	public ListMapColumn<Key,T> getColumn(int i) {
		return get(i);
	}
	
	public ListMapRow<Key,T> getRow(Key k) {
		return new ListMapRow<Key,T>(this,k);
	}
	
	public boolean containsKey(Object key) {
		if (size() == 0) {
			return false;
		}

		return get(0).containsKey(key);

	}

	public boolean containsValue(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public Set<java.util.Map.Entry<Key, ListMapRow<Key, T>>> entrySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get a row by its key
	 */
	@SuppressWarnings("unchecked")
	public ListMapRow<Key, T> get(Object key) {
		return new ListMapRow<Key, T>(this, (Key) key);
	}

	public Set<Key> keySet() {
		if (size() == 0) {
			return Functional.set();
		} else {
			return get(0).keySet();
		}
	}

	public ListMapRow<Key, T> put(Key arg0, ListMapRow<Key, T> arg1) {
		throw new UnsupportedOperationException(); // Use put(key, list<T>) instead
	}

	public ListMapRow<Key,T> put(Key key, List<T> values) {
		int n = size();
		if(n > 0 && values.size() != n) {
			throw new RuntimeException("Size mismatch: length of values list must equal number of ListMap columns");
		}
		
		if(containsKey(key)) {
			Iterator<T> lit = values.iterator();
			for(int i = 0; i < n; i++) {
				set(key, i, lit.next());
			}
		} else {
			if(n == 0) {
				for(int i = 0; i < values.size(); i++) {
					add(new ListMapColumn<Key,T>());
				}
			}
			n = Math.max(n,values.size());
			Iterator<T> lit = values.iterator();
			for(int i = 0; i < n; i++) {
				set(key, i, lit.next());
			}
		}
		return getRow(key);
	}
	
	public void putAll(Map<? extends Key, ? extends ListMapRow<Key, T>> arg0) {
		throw new UnsupportedOperationException();
	}

	public Collection<ListMapRow<Key, T>> values() {
		throw new UnsupportedOperationException();		
	}
}
