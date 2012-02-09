package org.princehouse.mica.base.net.base;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.util.NotImplementedException;


public class BaseConnection extends Connection {

	private InputStream inputStream;
	private OutputStream outputStream;

	
	public BaseConnection(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	@Override
	public void close() throws IOException {
		throw new NotImplementedException();
	}


	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}
	
}
