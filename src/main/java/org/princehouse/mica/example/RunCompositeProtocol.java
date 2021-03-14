package org.princehouse.mica.example;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;

public class RunCompositeProtocol {

  /**
   * Run an n-node simulation of the example MiCA gossip system FourLayerTreeStack. This class can
   * serve as an example of how to run simulations with MiCA protocols.
   * <p>
   * For available command line options, see the MicaOptions class
   *
   * @param args
   */
  public static void main(String[] args) {
    // Recommended program command line options:
    //    -stopAfter 100       (number of rounds to run the experiment)
    //    -n 25                (number of nodes simulated in the experiment)
    //
    // With default options, running this main method will create a log file in mica_log/ with
    // details of all gossip communications between nodes, in json format.
    TestHarness.main(args, new ProtocolInstanceFactory() {
      @Override
      public FourLayerTreeStack createProtocolInstance(int nodeId, Address address,
          Overlay overlay) {
        return new FourLayerTreeStack(overlay, nodeId);
      }
    });
  }

}
