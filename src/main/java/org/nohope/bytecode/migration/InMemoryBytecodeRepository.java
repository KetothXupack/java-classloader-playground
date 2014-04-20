package org.nohope.bytecode.migration;

import com.google.common.collect.Maps;

import java.util.*;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 04:14
 */
public class InMemoryBytecodeRepository implements Repository {
    private final Map<String, Map<Long, List<byte[]>>> bytecode = new HashMap<>();
    private final Map<String, Map<Long, Migration>> migrations = new HashMap<>();

    public void putBytecode(final String uniqueId, final long serialVersionUID, final Collection<byte[]> bytecode) {
        if (!this.bytecode.containsKey(uniqueId)) {
            this.bytecode.put(uniqueId, new HashMap<Long, List<byte[]>>());
        }
        this.bytecode.get(uniqueId).put(serialVersionUID, new ArrayList<>(bytecode));
    }

    public void putMigrations(final Migration... migrations) {
        for (final Migration migration : migrations) {
            final String uniqueId = migration.getUniqueId();
            if (!this.migrations.containsKey(uniqueId)) {
                this.migrations.put(uniqueId, Maps.<Long, Migration> newHashMap());
            }
            this.migrations.get(uniqueId).put(migration.getSourceSerialVersionUID(), migration);
        }
    }

    @Override
    public List<byte[]> getBytecode(final String uniqueId, final long serialVersionUID) {
        return bytecode.get(uniqueId).get(serialVersionUID);
    }

    @Override
    public List<Migration> getMigrations(final String uniqueId,
                                         final long sourceSerialVersionUID,
                                         final long targetSerialVersionUID) {
        final List<Migration> result = new ArrayList<>();
        final Map<Long, Migration> entityMigrations = migrations.get(uniqueId);
        long intermediateUID = sourceSerialVersionUID;
        while (intermediateUID != targetSerialVersionUID) {
            final Migration migration = entityMigrations.get(intermediateUID);
            result.add(migration);
            intermediateUID = migration.getTargetSerialVersionUID();
        }

        return result;
    }
}
