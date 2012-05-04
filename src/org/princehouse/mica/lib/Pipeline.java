package org.princehouse.mica.lib;

import java.util.LinkedList;
import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.util.Distribution;

public class Pipeline<P extends Protocol> extends BaseProtocol {
	
	public static abstract class ProtocolFactory<Q extends Protocol> {
		/*
		 * Called to create a new protocol instance for the pipeline
		 */
		public abstract Q createProtocol();
	
		/**
		 * Called immediately before a protocol is removed from the pipeline.
		 * Does nothing by default; you don't need to override this if you're
		 * cool with having old protocols be garbage collected.
		 * @param pinstance
		 */
		public void destroyProtocol(Q pinstance) {}
	}
	
	private static final long serialVersionUID = -3286147351797635135L;
	
	private int k;
	private ProtocolFactory<P> factory = null;
	private LinkedList<P> pipe = new LinkedList<P>();
	
	public LinkedList<P> getPipe() {
		return pipe;
	}

	public void setPipe(LinkedList<P> pipe) {
		this.pipe = pipe;
	}

	public Pipeline(int k, ProtocolFactory<P> factory) {
		this.k = k;
		this.factory = factory;
		for(int i = 0; i < k; i++) {
			pipe.add(factory.createProtocol());
		}
	}

	@Select
	public Distribution<Address> select() {
		return buildMerge().getSelectDistribution();
	}
	
	@GossipUpdate
	public void update(Pipeline<P> that) {
		buildMerge().executeUpdate(that.buildMerge());
	}
	
	@SuppressWarnings("unchecked")
	private Protocol buildMerge() {
		// merge together everything in the pipeline!
		return MergeCorrelated.merge( (List<Protocol>) pipe);	
	}
	
	@GossipRate
	public double rate() {
		return buildMerge().getFrequency();
	}
}
