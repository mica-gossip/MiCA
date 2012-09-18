package org.princehouse.mica.example.pulse;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.annotations.View;

/**
 * MO: Attempt to communicate round-robin with all nodes in the supplied overlay view
 * 
 * 
 * 
 * @author lonnie
 *
 */
public class StagedTrigger extends BaseProtocol {
	private static final long serialVersionUID = 1L;
	
	public Object view;
	
	public StagedTrigger(Object view) {
		this.view = view;
		
	}
	
	
	
	
}
