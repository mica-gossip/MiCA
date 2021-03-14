package org.princehouse.mica.base.net.dummy;

import java.io.IOException;

import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.net.model.NotBoundException;

public class DummyAddress implements Address {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String label = null;

    public DummyAddress(String label) {
        this.label = label;
    }

    @Override
    public int compareTo(Address o) {
        if (!(o instanceof DummyAddress)) {
            throw new UnsupportedOperationException();
        }
        return label.compareTo(((DummyAddress) o).label);
    }

    @Override
    public void bind(AcceptConnectionHandler h) throws IOException {
        throw new UnsupportedOperationException();

    }

    @Override
    public void unbind() throws NotBoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection openConnection() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DummyAddress)) {
            return false;
        }
        return label.equals(((DummyAddress) o).label);
    }

}
