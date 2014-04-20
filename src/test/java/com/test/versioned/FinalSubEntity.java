package com.test.versioned;

import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:11
 */
public class FinalSubEntity implements Serializable {
    private static final long serialVersionUID = 3L;

    private final String value;

    public FinalSubEntity(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
