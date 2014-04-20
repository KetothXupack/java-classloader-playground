package org.nohope.bytecode.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
* @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
* @since 2014-04-19 20:00
*/
public class RenamingObjectOutputStream extends ObjectOutputStream {
    private final Map<String, Class<?>> substitutes = new HashMap<>();

    public RenamingObjectOutputStream(final OutputStream out,
                                      final Map<String, Class<?>> substitutes) throws IOException {
        super(out);
        this.substitutes.putAll(substitutes);
    }

    @Override
    protected void writeClassDescriptor(final ObjectStreamClass desc) throws IOException {
        ObjectStreamClass out = desc;
        if (substitutes.containsKey(desc.getName())) {
            out = ObjectStreamClass.lookup(substitutes.get(desc.getName()));
        }
        super.writeClassDescriptor(out);
    }
}
