package org.princehouse.mica.base;

import org.princehouse.mica.base.sugar.annotations.View;

/**
 * ExternalSelectProtocol delegates its selection to a another object, of any type that the @Select
 * annotation accepts
 *
 * @author lonnie
 */
public abstract class ExternalSelectProtocol extends BaseProtocol {

  private static final long serialVersionUID = 1L;

  private Object select = null;

  @View
  final public Object getSelect() {
    return select;
  }

  public void setSelect(Object select) {
    this.select = select;
  }

  public ExternalSelectProtocol(Object select) {
    super();
    setSelect(select);
  }
}
