package org.princehouse.mica.base.model;

/**
 * Represents the "compiled" protocol to the MiCA Runtime.
 * <p>
 * A Compiler instance takes a Protocol implementation class and emits a RuntimeAgent for that
 * class. RuntimeAgent knows how to execute the distributed update function. Any analysis results
 * from the original Protocol class are stored in the RuntimeAgent.
 *
 * @param <P>
 * @author lonnie
 */
public abstract class RuntimeAgent {

  public RuntimeAgent() {
  }

  /**
   * Execute the gossip update with a remote peer.
   *
   * @param runtime
   *            Current runtime
   * @param pinstance
   *            Protocol instance
   * @param connection
   *            Open connection to the selected gossip peer
   */
  // public abstract void gossip(Runtime runtime, Protocol pinstance,
  // Connection connection) throws AbortRound, FatalErrorHalt;

}
