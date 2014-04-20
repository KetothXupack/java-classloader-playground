package com.test.versioned;

import org.nohope.bytecode.migration.Migratable;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:10
 */
@Migratable(
    uniqueId = "9df299e6-f594-4bb0-8a8a-8a03f44ba32f",
    dependencies = {FinalSubEntity.class}
)
public class FinalEntity implements Serializable {
    private static final long serialVersionUID = 3L;

    private final FinalSubEntity entity;

    public FinalEntity(final FinalSubEntity entity) {
        this.entity = entity;
    }

    public FinalSubEntity getEntity() {
        return entity;
    }
}
