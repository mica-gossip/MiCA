package org.princehouse.mica.base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.Sugar;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.MarkingObjectInputStream;

/**
 * Base for all MiCA protocols. Extend this class to create your own protocol.
 * 
 * @author lonnie
 * 
 */
public abstract class BaseProtocol implements Protocol, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7604139731035133018L;

    /**
     * Default constructor
     */
    public BaseProtocol() {
    }

    // Clunky mechanism to register "foreign" objects when they are deserialized
    // at a remote node
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (in instanceof MarkingObjectInputStream) {
            ((MarkingObjectInputStream) in).getForeignObjectSet().add(this);
        }
    }

    /**
     * RuntimeState stores information associated with a running protocol
     * instance, such as the current address and a random number generator seed.
     */
    @Override
    public RuntimeState getRuntimeState() {
        return MiCA.getRuntimeInterface().getRuntimeContextManager().getRuntimeState(this);
    }

    @Override
    public String toString() {
        try {
            return String.format("[%s@%s]", getClass().getName(), getRuntimeState().getAddress());
        } catch (RuntimeException e) {
            return "[!]";
        }
    }

    /**
     * The view contains probability-weighted list of neighbors for gossip.
     */
    @Override
    public Distribution<Address> getView() {
        return Sugar.v().executeSugarView(this);
    }

    /**
     * Rate is a multiplier for how fast a node gossips. getRate() is called by
     * the MiCA runtime to determine how long to wait between gossips.
     */
    @Override
    public double getRate() {
        return Sugar.v().executeSugarRate(this);
    }

    /**
     * Get the current node's address. This is part of RuntimeState. This method
     * is here for convenience; it is the same as getRuntimeState().getAddress()
     * 
     * @return Current node's address
     */
    @Override
    public Address getAddress() {
        return getRuntimeState().getAddress();
    }

    /**
     * Reserved for internal use. Write a message to the log. Log messages are
     * comma-separated fields of the format:
     * 
     * "local_timestamp,local_event_number,address,classname,name,MESSAGE"
     * 
     * Where MESSAGE is the result of String.format(formatStr,arguments).
     * 
     * @param formatStr
     * @param arguments
     */
    public static class InstanceLogObject {
        public Object data;
    }

    /**
     * Convenience method for logging only an event type Flags is an enum from
     * LogFlag enum.
     * 
     * @param flags
     *            An enum from LogFlag class; describes the category of the log
     *            message.
     * @param eventType
     *            An arbitrary string that will be written in the "event_type"
     *            attribute of the logged JSON message.
     */
    @Override
    public void logJson(Object flags, String eventType) {
        logJson(flags, eventType, null);
    }

    /**
     * @param flags
     *            An enum from LogFlag class; describes the category of the log
     *            message.
     * @param evenType
     *            An arbitrary string that will be written in the "event_type"
     *            attribute of the logged JSON message.
     * @param obj
     *            An object to include with the log message. MiCA will attempt
     *            to serialize the object into JSON, but this may fail for
     *            custom data types.
     */
    @Override
    public void logJson(Object flags, String eventType, final Object obj) {
        MiCA.getRuntimeInterface().logJson(flags, getAddress(), eventType, obj);
    }

    /**
     * The default rate of all protocols is 1.0. Override this only if you
     * specifically want to make this protocol gossip at a non-uniform rate
     * (i.e., merge operators do this)
     * 
     * @return Current gossip rate.
     */
    @GossipRate
    public double rate() {
        return 1.0;
    }

    /**
     * Returns an object that will be serialized when logging state update
     * messages. By default, this object is the protocol instance itself, but
     * overriding this method can serialize an arbitrary object instead.
     */
    @Override
    public Object getLogState() {
        return this;
    }

    /**
     * Called after getView, prior to initiating gossip. preUpdate will be
     * called even if the sampled address is null.
     */
    @Override
    public void preUpdate(Address selected) {
    }

    /**
     * Called on the node that initiates gossip after it has completed executing
     * the gossip exchange.
     */
    @Override
    public void postUpdate() {
    }

    /**
     * Called when MiCA fails to reach a peer for gossip, either due to timeout
     * or connection refused
     */
    @Override
    public void unreachable(Address peer) {
    }

    @Override
    public void busy(Address peer) {
    }

}
