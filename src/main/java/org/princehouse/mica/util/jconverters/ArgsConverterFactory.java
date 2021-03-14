package org.princehouse.mica.util.jconverters;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;
import org.princehouse.mica.base.net.model.Address;

public class ArgsConverterFactory implements IStringConverterFactory {

  @Override
  public Class<? extends IStringConverter<?>> getConverter(Class<?> forType) {
    if (forType.equals(Address.class)) {
      return AddressStringConverter.class;
    } else {
      return null;
    }
  }
}
