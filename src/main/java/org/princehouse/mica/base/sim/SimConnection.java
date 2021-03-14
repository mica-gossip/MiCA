package org.princehouse.mica.base.sim;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;

public class SimConnection extends Connection {

  @Override
  public InputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public Address getSrc() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Address getDst() {
    // TODO Auto-generated method stub
    return null;
  }

  public Protocol getRemoteNode() {
    // TODO Auto-generated method stub
    return null;
  }

}
