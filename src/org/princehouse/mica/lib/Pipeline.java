package org.princehouse.mica.lib;

import java.util.LinkedList;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.GossipRate;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
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
		// FIXME this only works if all protocols have the same distribution!!
		return pipe.get(0).getSelectDistribution();
	}
	
	@GossipUpdate
	public void update(Pipeline<P> that) {
		// execute update for every pair of local/remote protocols in the pipeline
		for(int i = 0; i < k; i++) {
			this.pipe.get(i).executeUpdate(that.pipe.get(i));
		}
		// destroy the oldest; create the newest
		P eldest = pipe.poll();
		factory.destroyProtocol(eldest);		
		pipe.add(factory.createProtocol());
	}
	
	@GossipRate
	public double rate() {
		// FIXME assumes all have the same frequency
		return pipe.get(0).getFrequency();
	}
}
