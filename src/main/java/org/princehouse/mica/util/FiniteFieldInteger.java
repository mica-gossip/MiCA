package org.princehouse.mica.util;

import java.util.Comparator;

public interface FiniteFieldInteger<E> {

  public Comparator<? super E> relativeDistanceComparator();

}
