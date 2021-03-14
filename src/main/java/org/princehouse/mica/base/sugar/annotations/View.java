package org.princehouse.mica.base.sugar.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * General purpose select annotation. Can be used to annotate a function returning
 * Distribution<Address>, Address (for a deterministic protocol), or a Distribution<Address>
 * member.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface View {

}
