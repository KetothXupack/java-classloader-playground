package org.nohope.serialization;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-22 11:29
 */
public class RenamingObjectInputStream extends ThreadContextClassLoaderAwareObjectInputStream {
    private final Map<String, Class<?>> substitutes = new HashMap<>();

    public RenamingObjectInputStream(final InputStream in, final Map<String, Class<?>> substitutes) throws IOException {
        super(in);
        this.substitutes.putAll(substitutes);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        final ObjectStreamClass objectStreamClass = super.readClassDescriptor();
        if (objectStreamClass != null && substitutes.containsKey(objectStreamClass.getName())) {
            return ObjectStreamClass.lookup(substitutes.get(objectStreamClass.getName()));
        }
        return objectStreamClass;
    }
}
