package org.nohope.bytecode.core;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.nohope.bytecode.ByteCodeUtils;
import org.nohope.bytecode.ClassLoaderUtils;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.Entry;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 22:21
 */
public class HidingClassLoader extends SecureClassLoader {
    private final Map<String, byte[]> classesByteCode;
    private final Map<String, byte[]> resources;
    private final Map<String, Class<?>> loadedClasses;

    public static Builder newBuilder() {
        return new Builder(ClassLoaderUtils.currentClassLoader());
    }

    public static Builder newBuilder(final ClassLoader parent) {
        return new Builder(parent);
    }

    public HidingClassLoader(final ClassLoader parent,
                             final Map<String, byte[]> bytecode,
                             final Map<String, byte[]> resources) {
        super(parent);
        this.classesByteCode = bytecode;
        this.resources = resources;
        this.loadedClasses = new HashMap<>(classesByteCode.size());
    }

    @Override
    public URL getResource(final String name) {
        if (resources.containsKey(name)) {
            try {
                final byte[] buf = resources.get(name);
                final URLStreamHandler handler = new InMemoryStreamHandler(new ByteArrayInputStream(buf));
                return new URL("in-memory-resource", null, -1, name, handler);
            } catch (final MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }

        return super.getResource(name);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (classesByteCode.containsKey(name)) {
                final Class<?> aClass = findClass(name);
                if (resolve) {
                    resolveClass(aClass);
                }
                return aClass;
            }
            return super.loadClass(name, resolve);
        }
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (!classesByteCode.containsKey(name)) {
            throw new ClassNotFoundException(name);
        }

        final byte[] b = classesByteCode.get(name);
        synchronized (loadedClasses) {
            if (loadedClasses.containsKey(name)) {
                return loadedClasses.get(name);
            }

            final Class<?> clazz = super.defineClass(name, b, 0, b.length);
            loadedClasses.put(name, clazz);
            return clazz;
        }
    }

    public Map<String, byte[]> getClassesByteCode() {
        return cloneMap(classesByteCode);
    }

    public static HidingClassLoader merge(final HidingClassLoader cl1,
                                          final HidingClassLoader cl2) throws ClassNotFoundException {
        return merge(ClassLoaderUtils.currentClassLoader(), cl1, cl2);
    }

    public static HidingClassLoader merge(final ClassLoader parent,
                                          final HidingClassLoader cl1,
                                          final HidingClassLoader cl2) throws ClassNotFoundException {
        if (!Sets.intersection(cl1.classesByteCode.keySet(), cl2.classesByteCode.keySet()).isEmpty()
            || !Sets.intersection(cl1.resources.keySet(), cl2.resources.keySet()).isEmpty()) {
            throw new IllegalArgumentException("Clashing resources and or classes found");
        }

        // make sure we'll not introduce new class definitions
        // of same classes in newly created class loader
        cl1.loadAll();
        cl2.loadAll();

        final HashMap<String, byte[]> resources = new HashMap<>();
        resources.putAll(cl1.resources);
        resources.putAll(cl2.resources);

        final HashMap<String, byte[]> lookup = new HashMap<>();
        lookup.putAll(cl1.classesByteCode);
        lookup.putAll(cl2.classesByteCode);

        final HidingClassLoader result = new HidingClassLoader(parent, lookup, resources);
        result.loadedClasses.putAll(cl1.loadedClasses);
        result.loadedClasses.putAll(cl2.loadedClasses);
        return result;
    }

    @Override
    public String toString() {
        return "<HidingClassLoader classes: '"
                + Joiner.on(":").join(classesByteCode.keySet())
                + "', resources: '"
                + Joiner.on(":").join(resources.keySet())
                + "'>"
                ;
    }

    private static Map<String, byte[]> cloneMap(final Map<String, byte[]> source) {
        final Map<String, byte[]> result = new HashMap<>(source.size());
        for (final Entry<String, byte[]> entry : source.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }
        return result;
    }

    private void loadAll() throws ClassNotFoundException {
        for (final String s : classesByteCode.keySet()) {
            findClass(s);
        }
    }

    public static class Builder {
        private final Map<String, byte[]> classesByteCode = new HashMap<>() ;
        private final Map<String, byte[]> resources = new HashMap<>();
        private final ClassLoader parent;


        private Builder(final ClassLoader parent) {
            this.parent = parent;
        }

        private Builder() {
            this(ClassLoaderUtils.currentClassLoader());
        }

        public Builder addByteCode(final Iterable<byte[]> bytecode) {
            for (final byte[] bytes : bytecode) {
                addByteCode(bytes);
            }

            return this;
        }

        public Builder addByteCode(final byte[] bytecode) {
            final Type type = ByteCodeUtils.getObjectType(bytecode);
            final String className = type.getClassName();
            final byte[] bytecodeClone = bytecode.clone();
            if (classesByteCode.put(className, bytecodeClone) != null
                    || resources.put(type.getInternalName() + ".class", bytecodeClone) != null) {
                throw new IllegalArgumentException("bytecode or resource for " + className + " already added");
            }

            return this;
        }

        public Builder addResource(final String resourceName, final byte[] resource) {
            final byte[] bytecodeClone = resource.clone();
            if (resources.put(resourceName, bytecodeClone) != null) {
                throw new IllegalArgumentException("resource " + resourceName + " already added");
            }

            return this;
        }

        public HidingClassLoader build() {
            return new HidingClassLoader(parent, classesByteCode, resources);
        }
    }
}
