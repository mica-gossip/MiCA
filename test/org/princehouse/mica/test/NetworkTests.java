package org.princehouse.mica.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.Test;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

public class NetworkTests {

  @Test
  public void testLocalhostLookup() throws UnknownHostException {
    InetAddress i = InetAddress.getByName("localhost");
    System.out.printf("Localhost %s\n", i);
  }

  // Test Address implementations
  @Test
  public void testTCPAddress() {
    AcceptConnectionHandler chandler = new AcceptConnectionHandler() {
      @Override
      public void acceptConnection(Address recipient, Connection connection) throws IOException {
        System.out.printf("Server %s: acceptConnection\n", recipient);

        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        ObjectOutputStream oos = new ObjectOutputStream(out);

        ObjectInputStream ois = new ObjectInputStream(in);

        try {
          Object m = ois.readObject();
          System.out.printf("Server receive: %s\n", m);

        } catch (ClassNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        String snd = "thank you";
        System.out.printf("Server send: %s\n", snd);
        oos.writeObject(snd);
        oos.close();

      }
    };

    try {
      InetAddress localhost = InetAddress.getByName("localhost");
      TCPAddress a = new TCPAddress(localhost, 8001);
      a.bind(chandler);
      Connection c = a.openConnection();
      subtestConnection(c);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      org.junit.Assert.fail();
    } catch (IOException e) {
      e.printStackTrace();
      org.junit.Assert.fail();
    }
  }

  public void subtestConnection(Connection c) throws IOException {
    OutputStream out = c.getOutputStream();
    InputStream in = c.getInputStream();

    ObjectInputStream ois = new ObjectInputStream(in);

    ObjectOutputStream oos = new ObjectOutputStream(out);
    String testmsg = "hello world";
    System.out.printf("Client send: %s\n", testmsg);
    oos.writeObject(testmsg);

    Object rcv = null;
    try {
      rcv = ois.readObject();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail();
    }
    String tmp = (String) rcv;
    System.out.printf("Client receive: %s\n", tmp);

    c.close();
  }

}
