package org.princehouse.mica.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.LeaderElection;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.Tree;
import org.princehouse.mica.util.Functional;


public class SpanningTree extends BaseProtocol implements Tree {

	private static final long serialVersionUID = 1L;

	// remember self-reported distance-from-root of neighbors
	private HashMap<Address,Integer> distanceFromRoot = new HashMap<Address,Integer>();


	private Set<Address> children = new HashSet<Address>();

	private Address parent = null;

	public LeaderElection leaderElection;

	@SelectUniformRandom
	public Overlay overlay;

	public SpanningTree(LeaderElection leaderElection, Overlay overlay) {
		this.leaderElection = leaderElection;
		this.overlay = overlay;
	}

	@Override
	public Collection<Address> getChildren() {
		return children;
	}

	@Override
	public Address getParent() {
		return parent;
	}

	private static final int MAXDIST = 1000;
	
	public Collection<Address> getKnown() {
		return distanceFromRoot.keySet();
	}
	
	public int distanceFromRoot(Address a) {
		if(isRoot() && a.equals(getAddress())) {
			return 0;
		} else {
			if(a.equals(getAddress())) {
				// compute my address from root:  nearest neighbor + 1
				int d = Integer.MAX_VALUE / 2;
				for(Address v : getKnown()) {
					d = Math.min(d, distanceFromRoot(v)+1);
				}
				return Math.min(MAXDIST,d);
			} else {
				if(distanceFromRoot.containsKey(a)) {
					return distanceFromRoot.get(a);
				} else {
					return MAXDIST;
				}
			}
		}
	}

	public int distanceFromRoot() {
		// what is MY distance from root?
		return distanceFromRoot(getAddress());
	}

	public Address computeParent() {
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


	@GossipUpdate
	public void update(SpanningTree other) {
		// record our neighbor's distance from the root
		subup(other);
		other.subup(this);
	}
		
	public void subup(SpanningTree other) {
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
		return leaderElection.isLeader() || (overlay.getView().size() == 0);
	}

	@Override
	public Collection<Address> getView() {
		Set<Address> view = new HashSet<Address>();
		Address parent = getParent();
		if(parent != null)
			view.add(parent);
		Functional.extend(view,getChildren());
		return view;
	}

	@Override
	public String getStateString() {
		String tmp = String.format("root=%s dist=%s parent=%s children=", isRoot(), getDistanceFromRoot(), getParent());
		for(Address c : getChildren()) {
			tmp += String.format("%s|",c);
		}
		return tmp;
	}

	@Override
	public Overlay getChildrenAsOverlay() {
		return new Tree.ChildOverlay(this);
	}

}
