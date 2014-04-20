package org.nohope.bytecode.core;

import org.nohope.serialization.ThreadContextClassLoaderAwareObjectInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

/**
* @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
* @since 2014-04-19 20:00
*/
public class RenamingStream extends ThreadContextClassLoaderAwareObjectInputStream {
    private final Map<String, Class<?>> substitutes = new HashMap<>();

    public RenamingStream(final InputStream in,
                          final Map<String, Class<?>> substitutes) throws IOException {
        super(in);
        this.substitutes.putAll(substitutes);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        final String name = desc.getName();
        if (substitutes.containsKey(name)) {
            return substitutes.get(name);
        }

        return super.resolveClass(desc);
    }
}
