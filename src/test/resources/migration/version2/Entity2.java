package com.test.versioned;

import org.nohope.bytecode.migration.Migratable;
import com.test.versioned.SubEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:10
 */
@Migratable(
        uniqueId = "9df299e6-f594-4bb0-8a8a-8a03f44ba32f",
        dependencies = {SubEntity.class}
)
public class Entity2 implements Serializable {
    private static final long serialVersionUID = 2L;
    private final SubEntity value;

    public Entity2(final SubEntity value) {
        this.value = value;
    }
}
