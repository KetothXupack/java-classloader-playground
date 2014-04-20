package org.nohope.bytecode;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 05:44
 */
public final class ByteCodeUtils {
    private ByteCodeUtils() {
    }

    public static byte[] getBytecode(final Class<?> data) throws IOException {
        final String path = Type.getInternalName(data) + ".class";
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final URL resource = contextClassLoader.getResource(path);

        if (resource == null) {
            throw new IOException("Unable to read bytecode of "
                    + data.getCanonicalName() + ": resource " + path
                    + " was not found in current class loader");
        }

        try (final InputStream inputStream = resource.openStream()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    public static Type getObjectType(final byte[] bytecode) {
        final ClassReader reader = new ClassReader(bytecode);
        return Type.getObjectType(reader.getClassName());
    }

    public static String getInternalName(final byte[] bytecode) {
        return getObjectType(bytecode).getInternalName();
    }

    public static String getCanonicalName(final byte[] bytecode) {
        return getObjectType(bytecode).getClassName();
    }
}
