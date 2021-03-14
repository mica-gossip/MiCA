package org.princehouse.mica.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class StreamUtil {
    public static byte[] readEntireInputStream(InputStream is) throws IOException {
        return IOUtils.toByteArray(is);
    }
}
