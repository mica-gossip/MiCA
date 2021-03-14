package org.princehouse.mica.example;

import java.util.Comparator;
import org.princehouse.mica.lib.abstractions.Overlay;

public class FindMinComparable<T extends Comparable<T>> extends FindMin<T> implements
    Comparator<T> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public FindMinComparable(T initialValue, Overlay overlay, Direction direction) {
    super(initialValue, overlay, direction);
  }

  @Override
  public int compare(T o1, T o2) {
    return o1.compareTo(o2);
  }
}
