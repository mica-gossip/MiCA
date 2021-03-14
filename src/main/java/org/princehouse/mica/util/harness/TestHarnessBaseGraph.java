package org.princehouse.mica.util.harness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.lib.abstractions.StaticOverlay;
import org.princehouse.mica.util.Functional;

import fj.F;

public abstract class TestHarnessBaseGraph implements TestHarnessGraph {
    public abstract F<Integer, List<Integer>> getNeighbors();

    private List<Address> addresses = null;

    public TestHarnessBaseGraph(List<Address> addresses) {
        this.addresses = addresses;
    }

    public TestHarnessBaseGraph(int n, F<Integer, Address> addressFunc) {
        this.addresses = new ArrayList<Address>();
        for (int i = 0; i < n; i++) {
            addresses.add(addressFunc.f(i));
        }
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    private Map<Address, List<Address>> cachedGraph = null;

    public int size() {
        return addresses.size();
    }

    private void cacheAddressNeighbors() {
        F<Integer, List<Integer>> f = getNeighbors();

        cachedGraph = Functional.map();

        Map<Address, Integer> lookup = Functional.map();
        for (int i = 0; i < addresses.size(); i++) {
            lookup.put(addresses.get(i), i);
        }

        for (Address a : addresses) {
            cachedGraph.put(a, Functional.list(Functional.map(f.f(lookup.get(a)), new F<Integer, Address>() {
                @Override
                public Address f(Integer i) {
                    return addresses.get(i);
                }
            })));
        }
    }

    public List<Address> getNeighbors(Address node) {
        if (cachedGraph == null) {
            cacheAddressNeighbors();
        }
        return cachedGraph.get(node);
    }

    public Overlay getOverlay(Address node) {
        return new StaticOverlay(getNeighbors(node));
    }

}
