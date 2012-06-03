package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.FindMinSymmetric;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.harness.TestHarness;

import fj.F3;

public class TestFindMinSymmetricNH extends TestHarness<FindMinSymmetric> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args)  {
	
		F3<Integer, Address, Overlay, FindMinSymmetric> createNodeFunc = new F3<Integer, Address, Overlay, FindMinSymmetric>() {
			@Override
			public FindMinSymmetric f(Integer i, Address address,
					Overlay neighbors) {
				FindMinSymmetric node = new FindMinSymmetric(i, neighbors.getView(null).keySet());
				//node.setName(String.format("Node%d",i));
				return node;
			}
		};

		new TestFindMinSymmetricNH().runMain(args, createNodeFunc);
		
	}

}
