package org.princehouse.mica.base.net.model;

import java.io.IOException;

public interface AcceptConnectionHandler {

	public void acceptConnection(Address recipient, Connection connection) throws IOException;
	
}
