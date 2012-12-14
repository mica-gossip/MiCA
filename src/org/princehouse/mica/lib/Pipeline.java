package org.princehouse.mica.lib;

import java.util.LinkedList;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.GossipRate;
import org.princehouse.mica.base.sugar.annotations.GossipUpdate;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.util.Distribution;

/*
 * The Pipeline class maintains a queue of k instances of a given protocol.
 * 
 * At the start of each gossip initiation, the oldest protocol instance is dequeued and a new instance queued.
 * 
 * Each round, the entire pipeline is merged with the MergeCorrelated operator, which builds a balanced tree of 
 * binary merge operations.  The gossip rate, select, and update operations are derived from this merge tree.
 * 
 * When every protocol in the pipeline has the same select distribution, the behavior is identical to Danny Dolev's
 * original pipeline --- the whole pipeline gossips in unison to a single selected neighbor, and the gossip rate is
 * preserved.
 * 
 * However, if pipelined instances have differing select distributions, the Pipeline's gossip rate will increase 
 * and instances will gossip individually or in smaller groups as necessary to preserve individual select distributions.
 * 
 * The Pipeline currently requires all Pipeline instances to be using the same k value.  Each round, the pipeline's length
 * will be adjusted to be exactly k in length.
 * 
 */

/**
 * @author lonnie
 * 
 * @param <P>
 */
public class Pipeline<P extends Protocol> extends BaseProtocol {
	private static final long serialVersionUID = -3286147351797635135L;

	/**
	 * 
	 * @param k
	 *            Number of instances to pipeline
	 * @param factory
	 *            Tells the pipeline how to create new protocol instances.
	 */
	public Pipeline(int k, ProtocolFactory<P> factory) {
		this.k = k;
		this.factory = factory;
		for (int i = 0; i < k; i++) {
			pipe.add(factory.createProtocol());
		}
	}

	/**
	 * Number of instances to pipeline
	 */
	private int k;

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	/**
	 * The ProtocolFactory is supplied to the Pipeline constructor. It tells the
	 * Pipeline how to create new protocol instances.
	 */
	private ProtocolFactory<P> factory = null;

	/**
	 * The Pipeline queue.
	 */
	private LinkedList<P> pipe = new LinkedList<P>();

	public LinkedList<P> getPipe() {
		return pipe;
	}

	public void setPipe(LinkedList<P> pipe) {
		this.pipe = pipe;
	}

	private Protocol merged = null;

	@View
	public Distribution<Address> select() {
		return getMerged().getView();
	}

	@Override
	public void preUpdate(Address address) {
		getMerged().preUpdate(address);
	}

	/**
	 * Called when a protocol has been polled out of the pipeline
	 * 
	 * @param protocol
	 */
	public void retire(P protocol) {
		factory.destroyProtocol(protocol);
	}

	private void advancePipeline() {
		while (pipe.size() > getK() - 1) {
			retire(pipe.poll());
		}

		while (pipe.size() < getK()) {
			pipe.addFirst(create());
		}
	}

	/**
	 * Called to create a new protocol instance to be added to the pipeline (by
	 * default, this calls the factory method)
	 */
	public P create() {
		return factory.createProtocol();
	}

	private Protocol getMerged() {
		if (merged == null) {
			// advance the pipeline
			advancePipeline();
			merged = buildMerge();
		}
		return merged;
	}

	@Override
	public void postUpdate() {
		getMerged().postUpdate();
		merged = null;
	}

	@GossipUpdate
	@Override
	public void update(Protocol other) {
		@SuppressWarnings("unchecked")
		Pipeline<P> that = (Pipeline<P>) other;
		// TODO handle discrepancies in pipeline sizes that could arise from
		// inconsistent k values
		getMerged().update(that.buildMerge());
	}

	@SuppressWarnings("unchecked")
	private Protocol buildMerge() {
		// merge together everything in the pipeline!
		return MergeCorrelated.operator.merge((List<BaseProtocol>) pipe);
	}

	@GossipRate
	public double rate() {
		merged = buildMerge();
		return merged.getRate();
	}

	/**
	 * 
	 * @author lonnie
	 * 
	 * @param <Q>
	 */
	public static abstract class ProtocolFactory<Q extends Protocol> {
		/*
		 * Called to create a new protocol instance for the pipeline
		 */
		public abstract Q createProtocol();

		/**
		 * Called immediately before a protocol is removed from the pipeline.
		 * Does nothing by default; you don't need to override this if you're
		 * cool with having old protocols be garbage collected.
		 * 
		 * @param pinstance
		 */
		public void destroyProtocol(Q pinstance) {
		}
	}
}
