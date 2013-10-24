package org.princehouse.mica.test;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

public class TestRate extends TestHarness {

    public static class TestProtocol extends BaseProtocol {
        private static final long serialVersionUID = 1L;

        public double rate = 1.0;
        
        @View
        public Overlay view = null;
        
        public TestProtocol(Overlay view, double rate) {
            this.rate = rate;
            this.view = view;
        }
        
        @Override
        public double getRate() {
            return rate;
        }
        
        @Override
        public void update(Protocol that) {
            // do nothing
        }
        
    }
    
    public static void main(String[] args) {
        TestHarness test = new TestRate();
        test.runMain(args);
    }

    // Override the default options. Command line flags will override these.
    @Override
    public MicaOptions defaultOptions() {
        MicaOptions options = super.defaultOptions();
        options.implementation = "sim"; // change to "sim" for simulator
        options.n = 25; // number of nodes to run
        options.graphType = "random";
        return options;
    }

    @Override
    public Protocol createProtocolInstance(int i, Address address, Overlay view) {
        double rate = 1.0 + (double)(i % 3);
        return new TestProtocol(view, rate);
    }
}
