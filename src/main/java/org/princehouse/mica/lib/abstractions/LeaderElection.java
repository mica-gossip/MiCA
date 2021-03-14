package org.princehouse.mica.lib.abstractions;

import org.princehouse.mica.base.net.model.Address;

/**
 * Leader Election interface
 *
 * @author lonnie
 */
public interface LeaderElection {

  /**
   * Returns the current leader belief
   *
   * @return Current leader
   */
  public Address getLeader();

  /**
   * @return True if local node believes it is leader. False otherwise.
   */
  public boolean isLeader();

}
