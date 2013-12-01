package org.princehouse.mica.example.syncpipe;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.util.Functional;

/**
 * Implements Danny Dolev's synchronizer pipeline. The basic scheme works like
 * this:
 * 
 * View = assumed to be uniform random over a complete graph. This
 * implementation takes an overlay.
 * 
 * Global, fixed state: int k = number of replicas int n = total number of nodes
 * int t = byzantine tolerance param. number of nodes that must reply before
 * advancing state = n - t
 * 
 * State at each node: sync - synchronizer state queue - contains k replica
 * stats r_i - replica state (specified separately)
 * 
 * 
 * 
 * @author lonnie
 * 
 */
public class SynchronizerPipeline<S extends Synchronizer> extends BaseProtocol {
    private static final long serialVersionUID = 1L;

    @View
    public Object view;

    private int n = 0;
    private int k = 0;
    private int t = 0;

    private ReplicaFactory<S> factory = null;
    private S synchronizer = null;

    // length of ListMap is k+1, since the synchronizer's remote state is stored
    // here too as the last column k
    private ListMap<Address, Object> data = new ListMap<Address, Object>();

    private LinkedList<Replica> replicas = null;

    public SynchronizerPipeline(Object view, int n, int k, int t, S synchronizer, ReplicaFactory<S> factory) {
        this.n = n;
        this.t = t;
        this.k = k;
        this.view = view;
        this.factory = factory;
        this.synchronizer = synchronizer;
        replicas = new LinkedList<Replica>();
        populateInitialReplicas();
    }

    private void populateInitialReplicas() {
        // TODO need more thought about how this initialization is actually
        // supposed to work
        for (int i = 0; i < k; i++) {
            replicas.add(factory.create(synchronizer));
        }
    }

    @GossipUpdate
    @Override
    public void update(Protocol other) {
        @SuppressWarnings("unchecked")
        SynchronizerPipeline<S> that = (SynchronizerPipeline<S>) other;
        // get vector of new values from that
        List<Object> updateVector = that.getUpdateVector();
        assert (updateVector.size() == k + 1);
        data.put(that.getAddress(), updateVector);

        if (data.keySet().size() >= n - t) {
            // once we've collected data from enough neighbors, advance the
            // synchronizer state and the pipeline

            Iterator<Replica> rit = replicas.iterator();
            Iterator<ListMapColumn<Address, Object>> dit = data.iterator();

            Replica r = rit.next();
            ListMapColumn<Address, Object> d = dit.next();

            // 1. update all replicas
            for (int i = 0; i < k; i++, r = rit.next()) {
                r.update(i, d);
            }

            // 2. update synchronizer and pop off the oldest replica
            ListMapColumn<Address, Object> synchronizerData = dit.next(); // should
                                                                          // be
                                                                          // the
                                                                          // last
                                                                          // entry
            synchronizer.update(replicas.pop(), synchronizerData);

            // 3. enqueue a new replica
            Replica newReplica = factory.create(synchronizer);
            replicas.add(0, newReplica);

            // 4. clear the collected data
            data.clear();
        }
    }

    public List<Object> getUpdateVector() {
        List<Object> v = Functional.list();
        // query replicas
        for (int i = 0; i < k; i++) {
            Replica r = replicas.get(i);
            v.add(r.getValue());
        }
        // query synchronizer
        v.add(synchronizer.getValue());
        return v;
    }
}
