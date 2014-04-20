package org.nohope.serialization;

import java.io.*;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-03-18 16:47
 */
public final class JavaProvider implements SerializationProvider {

    @Override
    public void writeObject(final OutputStream stream, final Serializable object) throws IOException {
        try (final ObjectOutput output = new ObjectOutputStream(stream)) {
            output.writeObject(object);
        }
    }

    @Override
    public <T extends Serializable> T readObject(final InputStream stream, final Class<T> clazz) throws IOException {
        try (final ObjectInput output = new ThreadContextClassLoaderAwareObjectInputStream(stream)) {
            return clazz.cast(output.readObject());
        } catch (final ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
