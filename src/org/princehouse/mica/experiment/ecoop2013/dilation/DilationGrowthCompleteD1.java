package org.princehouse.mica.experiment.ecoop2013.dilation;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.Dilator;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.harness.ProtocolInstanceFactory;
import org.princehouse.mica.util.harness.TestHarness;

/**
 * Compares dilation of PUSH vs PULL vs PUSHPULL for findmin on a complete graph
 * 
 * @author lonnie
 * 
 */
public class DilationGrowthCompleteD1 extends TestHarness implements ProtocolInstanceFactory {

    public int i;
    public int dilation = 0;
    public Protocol.Direction direction = Protocol.Direction.PUSHPULL;

    public static double minn = 10;
    public static double minpow = Math.log(minn) / Math.log(2);
    public static double maxn = 1000;
    public static double maxpow = Math.log(maxn) / Math.log(2);
    public static int experiments = 40;

    public static void main(String[] args) {
        for (int i = 0; i < experiments; i++) {
            DilationGrowthCompleteD1 test = new DilationGrowthCompleteD1(i);
            test.runMain(args);
        }
    }

    @Override
    public MicaOptions defaultOptions() {
        dilation = 0;

        MicaOptions options = super.defaultOptions();
        // options.n = 1000;
        options.implementation = "sim";
        options.graphType = "complete";
        options.timeout = 10000;
        options.simUpdateDuration = 0;
        // set very high to prevent high dilation from timing out and aborting
        options.roundLength = 100000;
        options.stagger = options.roundLength;
        options.stopAfter = 12;
        options.logsDisable = Functional.list(new String[] { "state", "rate", "select", "merge", "error" });
        options.reflectionCache = true;

        if (i > 0) {
            options.clearLogdir = false;
        }

        options.n = (int) Math.pow(2, minpow + (maxpow - minpow) * ((double) i) / ((double) (experiments - 1)));
        String graphname = options.graphType;
        if (graphname.equals("random"))
            graphname += String.format("-%s", options.rdegree);
        options.expname = String.format("%s_d%s_n%s", graphname, dilation, options.n);

        options.logdir = String.format("%s_%s", graphname, dilation);

        return options;
    }

    public DilationGrowthCompleteD1(int i) {
        this.i = i;
    }

    @Override
    public Protocol createProtocolInstance(int nodeId, Address address, Overlay overlay) {
        Protocol p = new FindMinChatty(nodeId, overlay, direction, getOptions().expname);
        if (dilation > 0) {
            p = Dilator.dilate(dilation, p);
        }
        return p;
    }

}
