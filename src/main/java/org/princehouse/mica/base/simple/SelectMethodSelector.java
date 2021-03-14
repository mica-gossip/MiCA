package org.princehouse.mica.base.simple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.util.Distribution;

/**
 * Works on any method that takes no parameters and returns an object of any type that @Select can
 * coerce into a Distribution<Address>
 *
 * @param <Protocol>
 * @author lonnie
 */
class SelectMethodSelector extends Selector {

  private Method method;

  public SelectMethodSelector(Method method) throws InvalidSelectElement {
    this.method = method;
    validate(method);
  }

  private void validate(Method Method) throws InvalidSelectElement {
    // FIXME: sanity check should go here
  }

  @Override
  public Distribution<Address> select(Protocol pinstance) throws SelectException {
    try {
      Object obj = method.invoke(pinstance);
      return Selector.asDistribution(obj, pinstance.getRuntimeState());
    } catch (IllegalArgumentException e) {
      throw new InvalidSelectElement(View.class, method);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return String.format("<%s Method %s>", getClass().getName(), method.getName());
  }
}