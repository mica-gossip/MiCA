package org.princehouse.mica.base.exceptions;

public class InvalidOption extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidOption(String option, Object value) {
    super(String.format("Invalid option for %s: \"\"", option, value));
  }
}
