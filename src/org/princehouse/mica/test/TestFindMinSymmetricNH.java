package org.princehouse.mica.test;

import java.net.UnknownHostException;
import java.util.List;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.example.FindMinSymmetric;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.TestHarness;

import fj.F3;

public class TestFindMinSymmetricNH extends TestHarness<FindMinSymmetric> {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args)  {
	
		
		F3<Integer, Address, List<Address>, FindMinSymmetric> createNodeFunc = new F3<Integer, Address, List<Address>, FindMinSymmetric>() {
			@Override
			public FindMinSymmetric f(Integer i, Address address,
					List<Address> neighbors) {
				FindMinSymmetric node = new FindMinSymmetric(i, Functional.set(neighbors));
				//node.setName(String.format("Node%d",i));
				return node;
			}
		};

		new TestFindMinSymmetricNH().runRandomGraph(0, 20, 8, createNodeFunc);
		
	}

}
