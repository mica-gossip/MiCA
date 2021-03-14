package org.princehouse.mica.base.exceptions;

import org.princehouse.mica.base.RuntimeErrorCondition;

public class FatalErrorHalt extends MicaException {

  public FatalErrorHalt(RuntimeErrorCondition condition, Throwable exception) {
    super(condition, exception);
  }

  public FatalErrorHalt() {
    super(null, null);
  }

  /**
   *
   */
  private static final long serialVersionUID = 1L;

}
