package org.princehouse.mica.util.harness;

import java.util.List;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;

public interface TestHarnessGraph {

  public Overlay getOverlay(Address node);

  public List<Address> getAddresses();
}
