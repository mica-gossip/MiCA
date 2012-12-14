package org.princehouse.mica.base.net.model;

import java.io.IOException;

import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;

public interface AcceptConnectionHandler {

	public void acceptConnection(Address recipient, Connection connection) throws IOException, FatalErrorHalt, AbortRound;
	
}
