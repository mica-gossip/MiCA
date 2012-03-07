package org.princehouse.mica.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Files {

	public static byte[] readEntireFile(File f) throws IOException {
		// TODO Auto-generated method stub
		int len = (int) f.length();
		int offset = 0;
		byte[] data = new byte[len];
		DataInputStream dis = new DataInputStream(new FileInputStream(f.getPath()));
		while(len > 0) {
			int b = dis.read(data, offset, len);
			offset += b; 
			len -= b;
		}
		dis.close();
		return data;
	}

	public static File createTempDir() {
		// This method taken from a comment on stackoverflow.com
		// http://stackoverflow.com/questions/375910/creating-a-temp-dir-in-java
		final String baseTempPath = System.getProperty("java.io.tmpdir");
		Random rand = new Random();
		int randomInt = 1 + rand.nextInt();
		File tempDir = new File(baseTempPath + File.separator + "tempDir" + randomInt);
		if (tempDir.exists() == false) { 
			tempDir.mkdir();
		}
		tempDir.deleteOnExit();
		return tempDir;
	}
}
