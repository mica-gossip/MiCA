package org.princehouse.mica.base.sugar.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Designates the gossip update function. Gossip update should take exactly one
 * parameter of the class type, and return void.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GossipUpdate {

}
