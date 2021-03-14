package org.princehouse.mica.base.model;

import org.princehouse.mica.base.simple.SelectException;

/**
 * Thrown by Selector.asDistribution and Overlay.getOverlay if they are called without a
 * RuntimeState object and it turns out to be necessary (not all objects will require RuntimeState
 * to produce a distribution)
 *
 * @author lonnie
 */
public class NeedsRuntimeException extends SelectException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

}
