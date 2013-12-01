package org.princehouse.mica.base;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

public class MalformedViewException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private Object source;
    private Distribution<Address> view;

    public MalformedViewException(Object source, Distribution<Address> view) {
        this.source = source;
        this.view = view;
    }

    public Distribution<Address> getView() {
        return view;
    }

    public void setView(Distribution<Address> view) {
        this.view = view;
    }

    public Object getProtocol() {
        return source;
    }

    public void setProtocol(Object source) {
        this.source = source;
    }

    public String toString() {
        return String.format("MalformedViewException: Source %s (class %s), view magnitude = %f", source,
                (source == null ? "null" : source.getClass().getName()), view.magnitude());
    }
}
