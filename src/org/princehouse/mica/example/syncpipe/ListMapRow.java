package org.princehouse.mica.example.syncpipe;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class ListMapRow<Key, T> implements List<T> {

    private ListMap<Key, T> lm = null;
    private Key key = null; // this listmaprow corresponds to the "key" entry of
                            // lm

    public ListMapRow(ListMap<Key, T> lm, Key key) {
        this.lm = lm;
        this.key = key;
    }

    @Override
    public boolean add(T arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int arg0, T arg1) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level

        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object arg) {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public T get(int i) {
        return lm.get(key, i);
    }

    @Override
    public int indexOf(Object arg) {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return lm.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        List<T> stat = getAsReadOnlyList();
        return stat.iterator();
    }

    /**
     * Get the column entries as a list. Modifying this list will NOT affect the
     * underlying ListMap, so it should be considered read-only
     * 
     * @return
     */
    private List<T> getAsReadOnlyList() {
        List<T> l = new LinkedList<T>();
        for (ListMapColumn<Key, T> col : lm) {
            l.add(col.get(key));
        }
        return l;
    }

    @Override
    public int lastIndexOf(Object arg0) {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator() {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int arg0) {
        // This is a read-only or modify-in-place operation, so could be
        // implemented.
        // However, it is not in the critical path, so it has been left as a
        // stub.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        // cannot change the length of an individual row -- all rows in the
        // ListMap must be the same length. To change the length, one must add
        // or
        // remove entire columns at the ListMap level
        throw new UnsupportedOperationException();
    }

    @Override
    public T set(int i, T x) {
        return lm.set(key, i, x);
    }

    @Override
    public int size() {
        return lm.size();
    }

    @Override
    public List<T> subList(int arg0, int arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> S[] toArray(S[] arg0) {
        throw new UnsupportedOperationException();
    }

}
