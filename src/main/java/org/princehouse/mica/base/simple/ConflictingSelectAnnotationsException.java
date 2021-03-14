package org.princehouse.mica.base.simple;

import java.lang.reflect.AnnotatedElement;

public class ConflictingSelectAnnotationsException extends SelectException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private AnnotatedElement element = null;

  public ConflictingSelectAnnotationsException(AnnotatedElement e) {
    this.element = e;
  }

  public String toString() {
    return String.format("Annotated element %s has conflicting MiCA annotations", element);
  }
}
