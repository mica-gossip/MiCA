package org.princehouse.mica.util.reflection;

import java.util.Set;
import org.princehouse.mica.base.model.Protocol;
import org.reflections.Reflections;

public class ReflectionUtil {

  public static <SomeType> Set<Class<? extends SomeType>> getImplementations(
      Class<SomeType> iface) {
    // FIXME currently only searches org.princehouse.mica... needs to extend
    // to user packages/classloaders too
    Reflections reflections = new Reflections("org.princehouse.mica");

    Set<Class<? extends SomeType>> subTypes = reflections.getSubTypesOf(iface);

    return subTypes;
  }

  public static Set<Class<? extends Protocol>> getAllProtocolClasses() {
    return getImplementations(Protocol.class);
  }

}
