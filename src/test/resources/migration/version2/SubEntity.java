package com.test.versioned;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:10
 */
public class SubEntity implements Serializable {
    private static final long serialVersionUID = 2L;

    private final int value;

    public SubEntity(final int value) {
        this.value = value;
    }
}
