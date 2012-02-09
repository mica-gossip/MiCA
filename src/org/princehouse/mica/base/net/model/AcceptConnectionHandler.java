package org.princehouse.mica.base.net.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface AcceptConnectionHandler {

	public void acceptConnection(Address recipient, Connection connection) throws IOException;
	
}
