package org.princehouse.mica.util;

public class ByteClassLoader extends ClassLoader {

  public static Class<?> define(String name, byte[] data) {
    return new ByteClassLoader().classFromBytes(name, data);
  }

  public Class<?> classFromBytes(String name, byte[] data) {
    return defineClass(name, data, 0, data.length);
  }
}
