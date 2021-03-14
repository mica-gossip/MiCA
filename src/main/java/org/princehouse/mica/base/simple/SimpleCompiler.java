package org.princehouse.mica.base.simple;

import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.Protocol;

/**
 * The SimpleCompiler and SimpleRuntime don't do any fancy analysis. They serialize the complete
 * state of the iniating node and send it to the receiver. The receiver computes update() and sends
 * the initiator's new state back to it.
 *
 * @author lonnie
 */
public class SimpleCompiler extends Compiler {

  public SimpleCompiler() {
  }

  private static CommunicationPatternAgent pattern = new SimpleCommunicationPatternAgent();

  /**
   * Not much going on here; for SimpleCompiler, the SimpleAgent does all the work.
   */
  @Override
  public CommunicationPatternAgent compile(Protocol pinstance) {
    return pattern;
  }
}
