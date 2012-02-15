package org.princehouse.mica.lib.abstractions;

import java.io.Serializable;

import org.princehouse.mica.base.model.Protocol;


/**
 * General interface for broadcast.
 * 
 * @author lonnie
 *
 * @param <Message>
 */
public interface Broadcast<Message extends Serializable> extends Protocol {
	
	public void sendMessage(Message m);
	
	public void receiveMessage(Message m);
	
}
