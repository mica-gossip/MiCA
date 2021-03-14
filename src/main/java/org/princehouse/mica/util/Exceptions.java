package org.princehouse.mica.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Exceptions {
    public static String stackTraceToString(Throwable t) {
        // from stack overflow
        // http://stackoverflow.com/questions/1149703/stacktrace-to-string-in-java
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }
}
