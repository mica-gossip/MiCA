package org.princehouse.mica.lib.abstractions;

import java.io.Serializable;
import org.princehouse.mica.base.MalformedViewException;
import org.princehouse.mica.base.model.NeedsRuntimeException;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.util.Distribution;

/**
 * An unchanging overlay. Used for protocols that do not change their views.
 *
 * @author lonnie
 */
public class StaticOverlay implements Overlay, Serializable {

  public StaticOverlay() {
  }

  private static final long serialVersionUID = 1L;

  private Object view = null;

  public StaticOverlay(Object view) {
    this.view = view;

    // sanity check
    try {
      Distribution<Address> sanityCheck = getView(null);
      if (sanityCheck != null && !sanityCheck.isOne()) {
        throw new MalformedViewException(view, sanityCheck);
      }
    } catch (NeedsRuntimeException e) {
      // ignore
    }

  }

  @Override
  public Distribution<Address> getView(RuntimeState rts) throws NeedsRuntimeException {
    try {
      return Selector.asDistribution(view, rts);
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
  }

  public void setView(Object view) {
    this.view = view;
  }

  @Override
  public Distribution<Address> getView() {
    try {
      return getView(null);
    } catch (NeedsRuntimeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

}
