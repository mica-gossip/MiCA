package org.princehouse.mica.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.ExternalSelectProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.RootedTree;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;


/**
 * Construct a rooted Spanning Tree overlay by using a leader election protocol to decide the root.
 * 
 * @author lonnie
 *
 */
public class SpanningTreeOverlay extends ExternalSelectProtocol implements RootedTree {

	private static final long serialVersionUID = 1L;

	// remember self-reported distance-from-root of neighbors
	private HashMap<Address,Integer> distanceFromRoot = new HashMap<Address,Integer>();


	private Set<Address> children = new HashSet<Address>();

	private Address parent = null;

	public LeaderElection leaderElection;

	/**
	 * Create a new instance
	 * 
	 * @param leaderElection  LeaderElection protocol instance
	 * @param sourceOverlay The overlay this algorithm gossips on
	 */
	public SpanningTreeOverlay(LeaderElection leaderElection, Overlay sourceOverlay) {
		super(sourceOverlay);
		this.leaderElection = leaderElection;
	}

	@Override
	public Collection<Address> getChildren() {
		return children;
	}

	@Override
	public Address getParent() {
		return parent;
	}

	private static final int MAXDIST = Integer.MAX_VALUE;
	
	private Collection<Address> getKnown() {
		return distanceFromRoot.keySet();
	}
	
	/**
	 * Get distance of a specified node to the root node
	 * Returns MAXDIST if unknown. 
	 * 
	 * @param address 
	 * @return
	 */
	public int distanceFromRoot(Address address) {
		if(address.equals(leaderElection.getLeader())) {
			return 0;
		} else {
			if(address.equals(getAddress())) {
				// compute my address from root:  nearest neighbor + 1
				int d = Integer.MAX_VALUE / 2;
				for(Address v : getKnown()) {
					d = Math.min(d, distanceFromRoot(v)+1);
				}
				return Math.min(MAXDIST,d);
			} else {
				if(distanceFromRoot.containsKey(address)) {
					return distanceFromRoot.get(address);
				} else {
					return MAXDIST;
				}
			}
		}
	}

	/**
	 * Local node's distance from root
	 * 
	 * @return
	 */
	public int distanceFromRoot() {
		// what is MY distance from root?
		return distanceFromRoot(getAddress());
	}

	private Address computeParent() {
		if(isRoot()) 
			return null;

		List<Address> view = Functional.extend(Functional.<Address>list(),getKnown());
		
		if(view.size() == 0)
			return null;
		
		Collections.sort(view, new Comparator<Address>() {
			@Override
			public int compare(Address a, Address b) {
				int t = ((Integer)distanceFromRoot(a)).compareTo((Integer)distanceFromRoot(b));
				if(t == 0) 
					t = a.compareTo(b); // break distance ties; sort by address
				return t;
			}});

		return view.get(0);
	}


	private int getDistanceFromRoot() {
		if(isRoot()) 
			return 0;

		Address p = getParent();
		if(p == null) {
			int mx = 0;
			for(int x : distanceFromRoot.values()) {
				mx = Math.max(x,mx);
			}
			if(mx == 0)
				return Integer.MAX_VALUE/2; // at least nobody will think we're the root...
			else
				return mx;
		}

		return distanceFromRoot.get(p) +1;	
	}


	/**
	 * Gossip update function
	 * 
	 * @param other
	 */
	@GossipUpdate
	public void update(SpanningTreeOverlay other) {
		// record our neighbor's distance from the root
		subup(other);
		other.subup(this);
	}
		
	// update helper function
	private void subup(SpanningTreeOverlay other) {
		distanceFromRoot.put(other.getAddress(), other.getDistanceFromRoot());
		
		parent = computeParent();
		children.remove(parent);
		
		if(getAddress().equals(other.getParent()))
			children.add(other.getAddress());
		else 
			children.remove(other.getAddress());
	}

	@Override
	public boolean isRoot() {
		return leaderElection.isLeader() || (getView().keySet().size() == 0);
	}

	@Override
	public Distribution<Address> getOverlay(RuntimeState rts) {
		Set<Address> view = new HashSet<Address>();
		Address parent = getParent();
		if(parent != null)
			view.add(parent);
		Functional.extend(view,getChildren());
		return Distribution.uniform(view);
	}


	@Override
	public Overlay getChildrenAsOverlay() {
		return new RootedTree.ChildOverlay(this);
	}

}
