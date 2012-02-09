package org.princehouse.mica.lib.abstractions;


import java.util.Collection;

import org.princehouse.mica.base.net.model.Address;


public interface Tree extends Overlay {

	public static class ChildOverlay implements Overlay {

		private Tree tree;
		public ChildOverlay(Tree t) {
			this.tree = t;
		}
		@Override
		public Collection<Address> getView() {
			return tree.getChildren();
		}
	}

	public Address getParent();
	
	public Collection<Address> getChildren();
	
	public boolean isRoot();

	public Overlay getChildrenAsOverlay();
}
