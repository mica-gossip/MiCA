package org.princehouse.mica.example.syncpipe;

public interface ReplicaFactory<S extends Synchronizer> {

    public Replica create(S synchronizerState);
}
