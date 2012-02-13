package org.princehouse.mica.base.net.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/** 
 * Connections provide an implementation-independent mechanism for 
 * streaming data to/from a remote node.
 * 
 * @author lonnie
 *
 */
public abstract class Connection {
	
	/**
	 * Get input stream from connection
	 * @return Input stream
	 * @throws IOException
	 */
	public abstract InputStream getInputStream() throws IOException;
	
	/**
	 * Get output stream from connection
	 * @return Output stream
	 * @throws IOException
	 */
	public abstract OutputStream getOutputStream() throws IOException;
	
	/**
	 * Close the connection. Note that the output stream may need to be flushed first.
	 * @throws IOException
	 */
	public abstract void close() throws IOException;

	/**
	 * Convenience method
	 * @return
	 * @throws IOException
	 */
	public PrintStream getOutputAsPrintStream() throws IOException {
		return new PrintStream(getOutputStream());
	}
}