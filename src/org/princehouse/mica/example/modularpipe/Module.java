package org.princehouse.mica.example.modularpipe;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;

/**
 * 
 * @author lonnie
 * 
 * @param <S>
 *            Summary type. Collected from individual nodes.
 * @param <A>
 *            Aggregate type.
 */
public abstract class Module<S, A> extends BaseProtocol {

    @View
    public Object view;

    public A aggregate;

    public Module(Object view, A initialAggregate) {
        this.view = view;
        this.aggregate = resetAggregate(initialAggregate);
    }

    public Module(Object view) {
        this(view, null);
    }

    @GossipUpdate
    public void update(Module<S, A> that) {
        S summary = that.getSummary();
        aggregate = updateAggregate(aggregate, summary);
        if (ready(aggregate)) {
            advance(that.getAddress(), aggregate);
            aggregate = resetAggregate(aggregate);
            advanced = true;
        }
    }

    /**
     * Set to true during update iff advance is called
     */
    public boolean advanced = true;

    /**
     * Returns true iff this module advanced since the last preUpdate (when
     * advanced is cleared)
     * 
     * @return
     */
    public boolean advanced() {
        return advanced;
    }

    public abstract boolean ready(A aggregate);

    /**
     * Reset the aggregate value after advance has been called
     * 
     * @param current
     * @return
     */
    public abstract A resetAggregate(A current);

    public abstract A updateAggregate(A aggregate, S summary);

    public abstract S getSummary();

    public abstract void advance(Address address, A aggregate);

    private static final long serialVersionUID = 1L;

    public void preUpdate() {
        advanced = false;
    }

}
