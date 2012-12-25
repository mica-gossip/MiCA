package org.princehouse.mica.example;

import org.princehouse.mica.base.ProtocolFactory;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;

public class EpochPipeline<P extends Protocol> extends MergeCorrelated {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ProtocolFactory<P> factory = null;
	private int epochLength = 0; // epoch length, in mica rounds
	
	private int currentEpochStart = 0;
	
	public EpochPipeline(ProtocolFactory<P> factory, int epochLength) {
		super(factory.create(), factory.create());
		this.factory = factory;
		this.epochLength = epochLength;
		currentEpochStart = getRuntimeState().getRound();
	}
	
	@SuppressWarnings("unchecked")
	public P get() {
		return (P) getP1();
	}
	
	@Override
	public void update(Protocol that) {
		if(getRuntimeState().getRound() - currentEpochStart >= epochLength) {
			// A new epoch has begun
			setP1(getP2());
			setP2(factory.create());
			currentEpochStart = getRuntimeState().getRound();
		}
		super.update(that);
	}

}
