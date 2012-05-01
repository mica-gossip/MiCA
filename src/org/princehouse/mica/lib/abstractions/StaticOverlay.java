package org.princehouse.mica.lib.abstractions;


import java.io.Serializable;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.util.Distribution;


/**
 * An unchanging overlay. Used for protocols that do not change their views.
 * 
 * @author lonnie
 *
 */
public class StaticOverlay implements Overlay, Serializable {

	private static final long serialVersionUID = 1L;
	
	public Object view = null;
	
	public StaticOverlay(Object view) {
		this.view = view;
	}
	
	@Override
	public Distribution<Address> getView() {
		try {
			return Selector.asDistribution(view);
		} catch (SelectException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setView(Object view) {
		this.view = view;
	}
	

}
