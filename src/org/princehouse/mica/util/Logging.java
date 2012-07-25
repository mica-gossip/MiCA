package org.princehouse.mica.util;

import org.princehouse.mica.base.net.model.Address;

/**
 * Contains subclasses for custom log events
 * @author lonnie
 *
 */
public class Logging {

	public static class SelectEvent {
		public Address selected = null;
		public Distribution<Address> view = null;
 	};
}
