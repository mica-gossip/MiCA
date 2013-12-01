package org.princehouse.mica.test.analysis;

import org.princehouse.mica.base.model.Protocol;

public class TestC1ProtocolProxyJava {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // C1TestProtocolProxy is syntesized by running TestProxyGenerator.main
        // import org.princehouse.mica.test.analysis.C1TestProtocolProxy
        C1TestProtocol a = new C1TestProtocol();
        C1TestProtocol b = new C1TestProtocol();

        System.out.println("test boxing");
        C1TestProtocolProxy proxy = new C1TestProtocolProxy();

        a.y = 1000;
        proxy.box(a);

        System.out.println(String.format("test proxy field box+set output value (expect 1000) = %d\n",
                proxy.getfield3()));

        proxy.setfield3(13);
        System.out
                .println(String.format("test proxy field get/set output value (expect 13) = %d\n", proxy.getfield3()));

        C1TestProtocolProxy nonproxy = new C1TestProtocolProxy();
        nonproxy.setTarget(b);
        b.y = 23;
        System.out.println(String.format("test nonproxy field get output value (expect 23) = %d\n",
                nonproxy.getfield3()));

        nonproxy.setfield3(11);
        System.out.println(String.format("test nonproxy field set output value (expect 11) = %d\n", b.y));

        System.out.println("test executeUpdate");
        proxy.executeUpdate(b);

        System.out.println("test applyDif");
        proxy.applyDiff(a);

    }

}
