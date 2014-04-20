package com.test.versioned.foo;

import java.io.Serializable;

import org.nohope.bytecode.migration.Migratable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:10
 */
@Migratable(
        uniqueId = "9df299e6-f594-4bb0-8a8a-8a03f44ba32f"
)
public class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int value;

    public Entity(final int value) {
        this.value = value;
    }
}
