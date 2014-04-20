package org.nohope.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:30
 */
public interface SerializationProvider {
    /**
     * Writes object to a given output stream.
     * Stream will be closed after method execution.
     */
    void writeObject(final OutputStream stream,
                     final Serializable object) throws IOException;

    /**
     * Reads object from a given input stream.
     * Stream will be closed after method execution.
     */
    <T extends Serializable> T readObject(final InputStream stream,
                                          final Class<T> clazz) throws IOException;
}
