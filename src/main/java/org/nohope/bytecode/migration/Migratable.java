package org.nohope.bytecode.migration;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:03
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Migratable {
    String uniqueId();
    Class<? extends Serializable>[] dependencies() default {};
}
