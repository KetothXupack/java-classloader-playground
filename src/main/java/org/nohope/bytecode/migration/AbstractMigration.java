package org.nohope.bytecode.migration;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 02:41
 */
public abstract class AbstractMigration implements Migration {
    private final String uniqueId;
    private final long sourceSerialVersionUID;
    private final long targetSerialVersionUID;

    protected AbstractMigration(final String uniqueId,
                                final long sourceSerialVersionUID,
                                final long targetSerialVersionUID) {
        this.uniqueId = uniqueId;
        this.sourceSerialVersionUID = sourceSerialVersionUID;
        this.targetSerialVersionUID = targetSerialVersionUID;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public long getSourceSerialVersionUID() {
        return sourceSerialVersionUID;
    }

    @Override
    public long getTargetSerialVersionUID() {
        return targetSerialVersionUID;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractMigration that = (AbstractMigration) o;
        return sourceSerialVersionUID == that.sourceSerialVersionUID
            && targetSerialVersionUID == that.targetSerialVersionUID
            && uniqueId.equals(that.uniqueId);

    }

    @Override
    public int hashCode() {
        int result = uniqueId.hashCode();
        result = 31 * result + (int) (sourceSerialVersionUID ^ (sourceSerialVersionUID >>> 32));
        result = 31 * result + (int) (targetSerialVersionUID ^ (targetSerialVersionUID >>> 32));
        return result;
    }
}
