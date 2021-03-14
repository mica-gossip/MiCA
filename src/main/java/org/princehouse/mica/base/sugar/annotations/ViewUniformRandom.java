package org.princehouse.mica.base.sugar.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Syntactic sugar. Tells MiCA to select uniformly at random from the annotated
 * Collection<Address>, Overlay, or function returning a collection of
 * addresses.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewUniformRandom {
}
