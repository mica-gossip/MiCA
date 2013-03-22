package org.princehouse.mica.test.analysis;

import org.princehouse.mica.base.model.Protocol;

public class TestC1ProtocolProxyJava {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// C1TestProtocolProxy is syntesized by running TestProxyGenerator.main
		// import org.princehouse.mica.test.analysis.C1TestProtocolProxy
		Protocol a = new C1TestProtocol();
		Protocol b = new C1TestProtocol();

		System.out.println("test boxing");
		C1TestProtocolProxy proxy = new C1TestProtocolProxy();

		C1TestProtocolProxy nonproxy = new C1TestProtocolProxy();
		nonproxy.setTarget(b);
		
		
		proxy.box(a);

		System.out.println("test field get/set");
		proxy.setfield3(13);

		System.out.println(String.format("   ---> output value = %d\n",
		proxy.getfield3()));

		
		System.out.println("test executeUpdate");
		proxy.executeUpdate(b);

		System.out.println("test applyDif");
		proxy.applyDiff(a);

	}

}
