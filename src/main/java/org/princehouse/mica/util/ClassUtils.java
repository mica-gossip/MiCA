package org.princehouse.mica.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;

public class ClassUtils {

  public static boolean debug = false;

  public static class RefCounter {

    static class Reference {

      public Object object;
      private String description;

      public Reference(String description, Object obj) {
        object = obj;
        this.description = description;
      }

      public String toString() {
        return description;
      }
    }

    Set<Object> seen = new HashSet<Object>();
    private Object offendingObject = null;

    public RefCounter() {
    }

    public boolean findReferenceCycles(Object obj) {
      seen.clear();
      offendingObject = null;
      List<Reference> path = visit(obj);

      if (debug) {
        System.err.printf(">>>> reference list result for %s\n", obj.getClass().getCanonicalName());
        if (path == null) {
          System.err.println("     null reference list!");
        } else {
          for (Reference r : path) {
            System.err.printf("     %s\n", r);
          }
        }
      }

      if (path == null) {
        return false;
      } else {
        path = Functional.prepend(path,
            new Reference(String.format("%s base object", obj.getClass().getCanonicalName()), obj));
        System.err.printf("Reference cycle found:\n");
        for (Reference r : path) {
          System.err.printf("  %s %s\n", r, (r.object == offendingObject ? "<-----" : ""));
        }
        return true;
      }

    }

    private boolean isPrimitiveRefType(Object obj) {
      return (obj instanceof Number) || (obj instanceof String);
    }

    private List<Reference> visit(Object obj) {
      List<Reference> rval = null;

      if (obj == null) {
        rval = null;
      } else if (isPrimitiveRefType(obj)) {
        rval = null;
      } else {
        if (seen.contains(obj)) {
          offendingObject = obj;
          rval = new LinkedList<Reference>();
        } else {
          seen.add(obj);
        }

        if (debug) {
          System.err.printf("    get references for %s\n", obj.getClass().getCanonicalName());
          if (obj instanceof Throwable) {
            System.err.println(
                "      weirdness: cycle check object is a throwable; printing stack trace");
            ((Throwable) obj).printStackTrace();
          }
        }

        int i = 0;
        for (Reference r : getReferences(obj)) {
          if (debug) {
            System.err.printf("      %d  ---> %s\n", i++, r);
          }

          List<Reference> temp = visit(r.object);
          if (temp != null) {
            temp = Functional.prepend(temp, r);
            rval = temp;
          }
        }
      }
      seen.remove(obj);

      if (debug) {
        if (rval != null) {
          for (Reference r : rval) {
            System.err.printf("%s\n", r);
          }
        }
      }
      return rval;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Reference> getReferences(Object obj) {
      List<Reference> temp = new LinkedList<Reference>();

      // traverse all attributes
      Class<?> k = obj.getClass();
      for (Field field : k.getDeclaredFields()) {
        // for(Field field : k.getFields()) {
        // ignore static fields
        if ((field.getModifiers() & Modifier.STATIC) != 0) {
          continue;
        }
        field.setAccessible(true);
        try {
          temp.add(
              new Reference(String.format("%s.%s", k.getName(), field.getName()), field.get(obj)));
        } catch (Exception e) {
          continue;
        }
      }

      if (obj instanceof Iterable) {
        int i = 0;
        for (Object element : (Iterable<Object>) obj) {
          temp.add(new Reference(String.format("%s.iterate[%d]", k.getName(), i++), element));
        }
      }

      if (obj instanceof Object[]) {
        int i = 0;
        for (Object element : (Object[]) obj) {
          temp.add(new Reference(String.format("%s.array[%d]", k.getName(), i++), element));
        }
      }

      if (obj instanceof Map) {
        temp.add(new Reference(String.format("%s.mapkeys", k.getName()), ((Map) obj).keySet()));
        temp.add(new Reference(String.format("%s.mapvalues", k.getName()), ((Map) obj).values()));
      }

      return temp;
    }
  }

  /**
   * returns true if a reference cycle is found, and prints out a reference path on standard error
   *
   * @param obj
   * @return
   */
  public static boolean findReferenceCycles(Object obj) {
    return new RefCounter().findReferenceCycles(obj);
  }

  public static boolean classImplements(Class<?> klass, Class<?> iface) {
    assert (iface.isInterface());
    for (Class<?> i : klass.getInterfaces()) {
      if (i.equals(iface)) {
        return true;
      }
    }
    if (klass.equals(java.lang.Object.class)) {
      return false;
    }
    return classImplements(klass.getSuperclass(), iface);
  }

  public static byte[] getClassBytecode(final Class<?> klass) throws IOException {
    String location = getClassLocation(klass);
    assert (location != null);
    return Files.readEntireFile(new File(location));
  }

  public static String getClassLocation(final Class<?> pClass) {
    final String location, name;
    name = pClass.getName().replaceAll("\\.", "/") + ".class";
    location = ClassLoader.getSystemResource(name).getPath();
    return location;
  }

  public static boolean classExtends(Class<?> k, Class<?> parentClass) {
    if (k.equals(parentClass)) {
      return true;
    }
    if (k.equals(java.lang.Object.class)) {
      return false;
    }
    return classExtends(k.getSuperclass(), parentClass);
  }

  /**
   * Return all methods from a class that are annotated with Exchange Use
   * getExchangeAnnotation(method) to get the annotation proxy.
   *
   * @param k A class.
   * @return List of methods that have an Exchange annotation.
   */
  public static List<Method> getExchangeMethods(Class<?> k) {
    List<Method> results = new LinkedList<Method>();
    for (Method m : k.getMethods()) {
      if (getExchangeAnnotation(m) != null) {
        results.add(m);
      }
    }
    return results;
  }

  /**
   * Return the Exchange annotation proxy from a method, or null if none exists.
   *
   * @param m A method.
   * @return Annotation proxy or null.
   */
  public static Annotation getExchangeAnnotation(Method m) {
    for (Annotation a : m.getAnnotations()) {
      Class<?> k = a.annotationType();
      if (k.equals(GossipUpdate.class)) {
        return a;
      }
    }
    return null;
  }

}
