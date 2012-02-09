package org.princehouse.mica.base.net.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public abstract class Connection {
	
	public abstract InputStream getInputStream() throws IOException;
	public abstract OutputStream getOutputStream() throws IOException;
	
	public abstract void close() throws IOException;

	// Convenience methods
	public PrintStream getOutputAsPrintStream() throws IOException {
		return new PrintStream(getOutputStream());
	}
}