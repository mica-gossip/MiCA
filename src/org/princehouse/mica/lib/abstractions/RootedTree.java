package org.princehouse.mica.lib.abstractions;


import java.io.Serializable;
import java.util.Collection;

import org.princehouse.mica.base.net.model.Address;


/**
 * Tree overlay abstraction
 * 
 * @author lonnie
 *
 */
public interface RootedTree extends Overlay {

	/**
	 * An overlay that causes nodes to gossip only to their children 
	 * @author lonnie
	 *
	 */
	public static class ChildOverlay implements Overlay, Serializable {

		private static final long serialVersionUID = 1L;
		private RootedTree tree;
		public ChildOverlay(RootedTree t) {
			this.tree = t;
		}
		@Override
		public Collection<Address> getView() {
			return tree.getChildren();
		}
	}

	/**
	 * Get local parent
	 * @return
	 */
	public Address getParent();
	
	/**
	 * Get local children
	 * @return
	 */
	public Collection<Address> getChildren();
	
	/**
	 * Is this node the root?
	 * @return
	 */
	public boolean isRoot();

	/**
	 * Return the set of children as an Overlay
	 * @return
	 */
	public Overlay getChildrenAsOverlay();
}
