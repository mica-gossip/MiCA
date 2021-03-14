package org.princehouse.mica.test;

import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;
import org.princehouse.mica.base.net.model.Address;
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

        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

}
