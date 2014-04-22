package org.nohope.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-21 02:23
 */
public interface SubstitutionSerializationProvider extends SerializationProvider {

    /**
     * Reads object from a given input stream replacing existing types from map of substitutions.
     * Stream should be closed after method execution.
     */
    <T extends Serializable> T readObject(final InputStream stream,
                                          final Class<T> clazz,
                                          final Map<String, Class<?>> substitutions) throws IOException;
}
