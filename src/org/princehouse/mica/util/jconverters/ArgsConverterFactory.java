package org.princehouse.mica.util.jconverters;

import org.princehouse.mica.base.net.model.Address;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;

public class ArgsConverterFactory implements IStringConverterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<? extends IStringConverter<T>> getConverter(Class<T> forType) {
		if(forType.equals(Address.class)) 
			return (Class<? extends IStringConverter<T>>) AddressStringConverter.class;
		else 
			return null;
	}

}
