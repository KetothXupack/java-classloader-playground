package org.nohope.bytecode.migration;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-19 19:05
 */
public interface TypedMigration<S extends Serializable> {
    Serializable migrate(final S source);
}
