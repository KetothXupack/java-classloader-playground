package org.nohope.bytecode.migration;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 02:34
 */
public interface Migration {
    String getUniqueId();
    long getSourceSerialVersionUID();
    long getTargetSerialVersionUID();
    Serializable migrate(final Serializable source) throws Exception;
}
