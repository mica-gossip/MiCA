package org.princehouse.mica.util.jconverters;

import org.princehouse.mica.base.net.model.Address;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;

public class ArgsConverterFactory implements IStringConverterFactory {
    @Override
    public Class<? extends IStringConverter<?>> getConverter(Class<?> forType) {
        if (forType.equals(Address.class))
            return AddressStringConverter.class;
        else
            return null;
    }
}
