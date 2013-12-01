package org.princehouse.mica.util.jconverters;

import java.net.UnknownHostException;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import com.beust.jcommander.IStringConverter;

/**
 * IStringConverter for JCommander to interpret strings as network addresses
 * 
 * @author lonnie
 * 
 */
public class AddressStringConverter implements IStringConverter<Address> {
    @Override
    public Address convert(String s) {
        try {
            return TCPAddress.valueOf(s);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
