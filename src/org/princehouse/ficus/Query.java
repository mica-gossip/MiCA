package org.princehouse.ficus;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import org.princehouse.mica.base.model.Protocol;

public abstract class Query implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Return true if a node is a source for a particular query
	 * @param p
	 * @return
	 */
	public abstract boolean isSource(Protocol p);
	public abstract QueryData generateSourceData(Protocol p);
	public abstract QueryData aggregate(List<QueryData> data) throws IrreducibleException;
	
	public int getId() {
		return id;
	}
	
	private int id;
	
	public Query() {
		id = new Random().nextInt();
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object other) {
		return (other instanceof Query && ((Query)other).getId() == this.getId());
	}
	
}
