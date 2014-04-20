package org.nohope.serialization;

import org.nohope.bytecode.ClassLoaderUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 02:03
 */
public class ThreadContextClassLoaderAwareObjectInputStream extends ObjectInputStream {
    public ThreadContextClassLoaderAwareObjectInputStream(final InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        final ClassLoader cl = ClassLoaderUtils.currentClassLoader();
        try {
            return cl.loadClass(desc.getName());
        } catch (final Exception ignored) {
        }

        return super.resolveClass(desc);
    }
}
