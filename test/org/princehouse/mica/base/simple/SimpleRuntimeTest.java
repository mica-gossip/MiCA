package org.princehouse.mica.base.simple;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class SimpleRuntimeTest {

  @Test
  public void testIntegerSerDe() {
    for (int i : Arrays.asList(3211, 0, 3138, 3135, Integer.MAX_VALUE, -1, Integer.MIN_VALUE)) {
      int roundTrip = SimpleRuntime.deserializeInteger(SimpleRuntime.serializeInteger(i));
      Assert.assertEquals(i, roundTrip);
    }
  }
}