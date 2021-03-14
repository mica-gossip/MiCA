package org.princehouse.mica.util;

import java.util.Iterator;

/**
 * Iterator interface for iterators that do not support the remove operation This exists only for
 * convenience --- one less stub to throw UnsupportedOperationException when you're writing an
 * iterator class that can't remove.
 *
 * @param <T>
 * @author lonnie
 */
public abstract class ImmutableIterator<T> implements Iterator<T> {

  @Override
  public final void remove() {
    throw new UnsupportedOperationException();
  }
}
