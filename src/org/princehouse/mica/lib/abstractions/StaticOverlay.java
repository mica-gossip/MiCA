package org.princehouse.mica.lib.abstractions;


import java.io.Serializable;
import java.util.Collection;

import org.princehouse.mica.base.net.model.Address;


/**
 * An unchanging overlay. Used for protocols that do not change their views.
 * 
 * @author lonnie
 *
 */
public class StaticOverlay implements Overlay, Serializable {

	private static final long serialVersionUID = 1L;
	private Collection<Address> view = null;
	
	public StaticOverlay(Collection<Address> view) {
		this.view = view;
	}
	
	@Override
	public Collection<Address> getView() {
		return view;
	}

}
