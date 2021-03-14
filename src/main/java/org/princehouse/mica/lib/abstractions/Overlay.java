package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.util.Distribution;

/**
 * The Overlay object is implemented by classes that export a view for use by a Protocol.  For
 * example, a protocol that builds an overlay network might implement the Overlay interface; this
 * could then be passed
 * <p>
 * Overlay can be used in place of getView using the @View syntactic sugar:
 *
 * @author lonnie
 * @View public Overlay view;
 */
public interface Overlay {

  /**
   * Get the Overlay's exported view
   *
   * @return A view
   */
  public Distribution<Address> getView();

  /**
   * This method is used by the MiCA runtime, but users should use the no-parameter variant. Returns
   * the exported view, which may be dependent on protocol runtime state.
   *
   * @param rts A protocol instance's bound runtime state
   * @return
   * @throws SelectException
   */
  public Distribution<Address> getView(RuntimeState rts) throws SelectException;

}
