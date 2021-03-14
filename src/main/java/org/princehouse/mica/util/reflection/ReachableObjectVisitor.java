package org.princehouse.mica.util.reflection;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.princehouse.mica.util.Functional;
import soot.Modifier;

public abstract class ReachableObjectVisitor {

  private Set<Object> visited = new HashSet<Object>();

  public ReachableObjectVisitor() {
  }

  /**
   * Analyze an object, calling visit once on each unique reachable non-primitive object
   *
   * @param root
   */
  public void analyze(final Object root) {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<ReachableObjectVisitor>() {
        @Override
        public ReachableObjectVisitor run() throws Exception {
          analyzeHelper(root);
          return null;
        }

      });
    } catch (PrivilegedActionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void analyzeHelper(Object root) {
    visited.clear();

    Stack<Object> s = new Stack<Object>();
    s.push(root);

    while (s.size() > 0) {
      Object o = s.pop();
      if (o == null || visited.contains(o) || o.getClass().isPrimitive()) {
        continue;
      }
      visit(o);
      visited.add(o);
      pushChildren(o, s);
    }
  }

  /**
   * Called once for each unique object encountered
   *
   * @param reachable
   */
  public abstract void visit(Object reachable);

  @SuppressWarnings("rawtypes")
  public void pushChildren(Object o, Stack<Object> stack) {
    if (o == null) {
      return;
    } else if (o instanceof Object[]) {
      Object[] array = (Object[]) o;
      if (array.getClass().getComponentType().isPrimitive()) {
        return;
      } else {
        for (Object c : array) {
          stack.push(c);
        }
      }
    } else if (o.getClass().isPrimitive()) {
      return;
    } else {
      // normal objects, inc. collections
      Class<?> klass = o.getClass();
      for (Field field : getAllFields(klass)) {
        if (field.getType().isPrimitive()) {
          continue;
        } else {
          try {
            stack.push(field.get(o));
          } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }

      if (o instanceof Iterable) {
        for (Object c : (Iterable) o) {
          stack.push(c);
        }
      }
    }

  }

  private static Map<Class<?>, List<Field>> allFieldsCache = Functional.map();

  /**
   * Get all fields, including private, inherited, and inherited private
   *
   * @param klass
   * @return
   */
  public static List<Field> getAllFields(Class<?> klass) {
    if (allFieldsCache.containsKey(klass)) {
      return allFieldsCache.get(klass);
    } else {
      List<Field> rval = null;
      if (klass.equals(Object.class) || klass.getName().startsWith("java.")) {
        // recursive base
        rval = Functional.list();
      } else {
        rval = Functional.list(klass.getDeclaredFields());
        Class<?> superklass = klass.getSuperclass();
        Functional.extend(rval, getAllFields(superklass));
      }

      for (Field field : rval) {
        if (!Modifier.isPublic(field.getModifiers())) {
          field.setAccessible(true);
        }
      }

      allFieldsCache.put(klass, rval);
      return rval;
    }
  }

}
