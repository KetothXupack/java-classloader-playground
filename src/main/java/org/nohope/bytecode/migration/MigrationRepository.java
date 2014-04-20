package org.nohope.bytecode.migration;

import java.util.List;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 05:33
 */
public interface MigrationRepository {
    List<Migration> getMigrations(final String uniqueId,
                                  final long sourceSerialVersionUID,
                                  final long targetSerialVersionUID);
}
