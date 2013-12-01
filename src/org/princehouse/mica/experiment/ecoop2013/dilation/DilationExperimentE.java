package org.princehouse.mica.experiment.ecoop2013.dilation;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;

/**
 * Test of dilation of findmin protocol on singly-linked ring
 * 
 * @author lonnie
 * 
 */
public class DilationExperimentE extends TestHarness implements ProtocolInstanceFactory {

    public static void main(String[] args) {
        for (int dilation = 0; dilation < 6; dilation += 2) {
            DilationExperimentE test = new DilationExperimentE(dilation);
            MicaOptions options = test.parseOptions(args);
            if (dilation > 0) {
                options.clearLogdir = false;
            }
            options.expname = "abcdefghijklmnop".substring(dilation, dilation + 1);
            test.runMain(options, test);
        }
    }

    @Override
    public MicaOptions defaultOptions() {
        MicaOptions options = super.defaultOptions();
        options.n = 1000;
        options.implementation = "sim";
        options.graphType = "singlering";
        options.timeout = 10000;
        // set very high to prevent high dilation from timing out and aborting
        options.roundLength = 100000;
        options.stagger = options.roundLength;
        options.stopAfter = 300;
        options.logsDisable = Functional.list(new String[] { "state", "rate", "select", "merge", "error" });
        return options;
    }

    public DilationExperimentE(int dilation) {
        this.dilation = dilation;
        direction = Protocol.Direction.PUSHPULL;
    }

    public int dilation = 0;
    public Protocol.Direction direction;

    @Override
    public Protocol createProtocolInstance(int nodeId, Address address, Overlay overlay) {
        String expname = MiCA.getOptions().expname;
        Protocol p = new FindMinChatty(nodeId, overlay, direction, String.format("%s-dilation-%s", expname, dilation));
        if (dilation > 0) {
            p = Dilator.dilate(dilation, p);
        }
        return p;
    }

}
