package org.princehouse.mica.lib.abstractions;

import java.util.List;
import org.princehouse.mica.base.model.Protocol;

public interface CompositeProtocol extends Protocol {

  public void addSubProtocol(Protocol p);

  public List<Protocol> getSubProtocols();
}

