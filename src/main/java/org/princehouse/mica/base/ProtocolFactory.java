package org.princehouse.mica.base;

import org.princehouse.mica.base.model.Protocol;

public interface ProtocolFactory<T extends Protocol> {

  public T create();
}
