package org.princehouse.mica.test.analysis;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.util.reflection.ReflectionUtil;

public class TestEnumerateProtocols {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(Class<? extends Protocol> c : ReflectionUtil.getAllProtocolClasses()) {
			System.out.println(c.getCanonicalName());
		}
	}

}
