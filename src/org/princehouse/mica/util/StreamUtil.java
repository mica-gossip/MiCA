package org.princehouse.mica.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class StreamUtil {
	public static InputStream bufferCompleteInputStream(InputStream is) throws IOException {
		byte[] bytes = IOUtils.toByteArray(is);
		System.out.printf("debug: inputStream is %d bytes\n",bytes.length);
		is.close();
		return new ByteArrayInputStream(bytes);
	}
}
