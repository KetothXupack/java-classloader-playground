package org.nohope.bytecode.migration;

import java.util.List;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 04:10
 */
public interface BytecodeRepository {
    List<byte[]> getBytecode(final String uniqueId, final long serialVersionUID);
}
