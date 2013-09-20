package org.princehouse.mica.lib.abstractions;

import java.io.Serializable;
import java.util.Collection;

import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

/**
 * Tree overlay abstraction
 * 
 * @author lonnie
 * 
 */
public interface RootedTree extends TreeOverlay {

    /**
     * An overlay that causes nodes to gossip only to their children
     * 
     * @author lonnie
     * 
     */
    public static class ChildOverlay implements Overlay, Serializable {

        public ChildOverlay() {
        }

        private static final long serialVersionUID = 1L;
        private RootedTree tree;

        public ChildOverlay(RootedTree t) {
            this.tree = t;
        }

        @Override
        public Distribution<Address> getOverlay(RuntimeState rts) {
            return Distribution.uniform(tree.getChildren());
        }
    }

    /**
     * Get local parent
     * 
     * @return
     */
    public Address getParent();

    /**
     * Get local children
     * 
     * @return
     */
    public Collection<Address> getChildren();

    /**
     * Is this node the root?
     * 
     * @return
     */
    public boolean isRoot();

    /**
     * Return the set of children as an Overlay
     * 
     * @return
     */
    public Overlay getChildrenAsOverlay();
}
