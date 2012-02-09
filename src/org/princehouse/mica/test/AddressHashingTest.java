package org.princehouse.mica.test;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;


import org.junit.Test;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public class AddressHashingTest {
	@Test
	public void testAddressEquality() throws UnknownHostException {
		Address a = TCPAddress.valueOf("localhost:8002");
		Address b = TCPAddress.valueOf("localhost:8002");
		
		Assert.assertTrue(a.equals(b));
	}
	
	@Test
	public void testAddressHash() throws UnknownHostException {
		Address a = TCPAddress.valueOf("localhost:8002");
		Address b = TCPAddress.valueOf("localhost:8002");
		
		Assert.assertEquals(a.hashCode(),b.hashCode());
	}
		
}
