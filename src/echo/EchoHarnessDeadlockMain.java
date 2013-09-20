package echo;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

public class EchoHarnessDeadlockMain extends TestHarness {

    public static void main(String[] args) {
        TestHarness test = new EchoHarnessDeadlockMain();
        test.runMain(args);
    }

    // Override the default options. Command line flags will override these.
    @Override
    public MicaOptions defaultOptions() {
        MicaOptions options = super.defaultOptions();
        options.implementation = "simple"; // change to "sim" for simulator
        options.n = 2; // number of nodes to run
        options.graphType = "complete";
        return options;
    }

    @Override
    public Protocol createProtocolInstance(int i, Address address, Overlay view) {
        return new Echo(view);
    }
}
