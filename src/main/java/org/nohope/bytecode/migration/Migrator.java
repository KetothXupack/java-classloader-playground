package org.nohope.bytecode.migration;

import org.nohope.bytecode.ByteCodeUtils;
import org.nohope.bytecode.ClassLoaderUtils;
import org.nohope.bytecode.core.HidingClassLoader;
import org.nohope.serialization.SerializationProvider;
import org.objectweb.asm.*;

import java.io.*;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Binary format idea:
 * <pre>
 *     +-------------------------------------+
 *     | framework version (long)            |
 *     +-------------------------------------+
 *     | uniqueId (int; char[])              |
 *     +-------------------------------------+
 *     | dependencies (int; (int; byte[]))[] | //maybe not necessary at all?
 *     +-------------------------------------+
 *     | serialized object (byte[])          |
 *     +-------------------------------------+
 * </pre>
 *
 *
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 21:29
 */
public class Migrator implements SerializationProvider {
    private static final long API_VERSION = 0x0001;

    private final Repository repository;
    private final SerializationProvider delegate;

    public Migrator(final SerializationProvider delegate, final Repository repository) {
        this.delegate = delegate;
        this.repository = repository;
    }

    @Override
    public void writeObject(final OutputStream stream,
                            final Serializable object) throws IOException {
        final DataOutputStream dos = new DataOutputStream(stream);

        final Class<? extends Serializable> clazz = object.getClass();
        final Migratable annotation = getAnnotation(clazz);

        dos.writeLong(API_VERSION);
        writeString(annotation.uniqueId(), dos);

        final Class<? extends Serializable>[] dependencies = annotation.dependencies();
        dos.writeInt(dependencies.length + 1);

        writeBytecode(clazz, dos);
        for (final Class<? extends Serializable> dependency : dependencies) {
            writeBytecode(dependency, dos);
        }

        delegate.writeObject(dos, object);
    }

    @Override
    public <T extends Serializable> T readObject(final InputStream stream,
                                                 final Class<T> clazz) throws IOException {
        try {
            return readObjectUnsafe(stream, clazz);
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T readObjectUnsafe(final InputStream stream,
                                                       final Class<T> clazz) throws Exception {
        final DataInputStream dataInputStream = new DataInputStream(stream);

        readApiVersion(dataInputStream); // TODO: some migrations based on api bytecode changes?

        final Migratable annotation = getAnnotation(clazz);
        final String uniqueId = readString(dataInputStream);
        if (!annotation.uniqueId().equals(uniqueId)) {
            throw new IllegalStateException();
        }

        final Collection<byte[]> byteCodes = new LinkedHashSet<>();
        final int dependencyCount = dataInputStream.readInt();
        for (int i = 0; i < dependencyCount; i++) {
            byteCodes.add(readBytecode(dataInputStream));
        }

        final HidingClassLoader sourceClassLoader = ClassLoaderUtils.classLoader(byteCodes);
        final byte[] sourceByteCode = byteCodes.iterator().next();
        final String sourceClassFQN = ByteCodeUtils.getCanonicalName(sourceByteCode);

        final Class<?> sourceClass = sourceClassLoader.loadClass(sourceClassFQN);
        final Serializable source = ClassLoaderUtils.runUsingClassLoader(sourceClassLoader,
                new Callable<Serializable>() {
                    @Override
                    public Serializable call() throws IOException {
                        // TODO: read embedded annotation from stream? (imagine api version changes)
                        final Migratable oldAnnotation = sourceClass.getAnnotation(Migratable.class);
                        return delegate.readObject(dataInputStream, (Class<? extends Serializable>) sourceClass);
                    }
                });

        final long sourceUID = getSerialVersionUID(sourceByteCode);
        final long targetUID = getSerialVersionUID(clazz);

        final AtomicReference<Serializable> intermediate = new AtomicReference<>(source);
        for (final Migration migration : repository.getMigrations(uniqueId, sourceUID, targetUID)) {
            final long intermediateTargetUID = migration.getTargetSerialVersionUID();
            final List<byte[]> bytecode = repository.getBytecode(migration.getUniqueId(), intermediateTargetUID);

            // at the final step we should ensure we executing
            // migration in current class loader
            if (intermediateTargetUID == targetUID) {
                intermediate.set(migration.migrate(intermediate.get()));
            } else {
                final ClassLoader cl = ClassLoaderUtils.classLoader(bytecode);
                intermediate.set(ClassLoaderUtils.runUsingClassLoader(cl,
                        ApplyMigration.to(migration, intermediate.get())
                ));
            }
        }

        return (T) intermediate.get();
    }

    private static class ApplyMigration implements Callable<Serializable> {
        private final Migration migration;
        private final Serializable object;

        private ApplyMigration(Migration migration, Serializable object) {
            this.migration = migration;
            this.object = object;
        }

        @Override
        public Serializable call() throws Exception {
            // TODO: validate migrated object
            return migration.migrate(object);
        }

        public static ApplyMigration to(final Migration migration,
                                        final Serializable object) {
            return new ApplyMigration(migration, object);
        }
    }

    private static long readApiVersion(final DataInput stream) throws IOException {
        final long apiVersion = stream.readLong();
        if (apiVersion > API_VERSION) {
            throw new IllegalArgumentException("Unsupported version " + apiVersion);
        }

        return apiVersion;
    }

    private static Migratable getAnnotation(final Class<? extends Serializable> clazz) {
        final Migratable annotation = clazz.getAnnotation(Migratable.class);
        if (annotation == null) {
            throw new IllegalArgumentException(clazz.getCanonicalName() + " have no @Migratable annotation");
        }
        return annotation;
    }

    private static byte[] readBytecode(final DataInput stream) throws IOException {
        final int bytecodeLength = stream.readInt();
        final byte[] bytecode = new byte[bytecodeLength];
        stream.readFully(bytecode, 0, bytecodeLength);
        return bytecode;
    }

    private static String readString(final DataInput stream) throws IOException {
        final int length = stream.readInt();
        final char[] chars = new char[length];

        for (int i = 0; i < length; i++) {
            chars[i] = stream.readChar();
        }

        return new String(chars);
    }

    private static void writeString(final String data, final DataOutput stream) throws IOException {
        stream.writeInt(data.length());
        for (final char c : data.toCharArray()) {
            stream.writeChar(c);
        }
    }

    private static void writeBytecode(final Class<?> clazz, final DataOutput stream) throws IOException {
        final byte[] bytes = ByteCodeUtils.getBytecode(clazz);

        stream.writeInt(bytes.length);
        stream.write(bytes, 0, bytes.length);
    }

    private static long getSerialVersionUID(final byte[] bytecode) {
        final ClassReader reader = new ClassReader(bytecode);

        final AtomicReference<Long> result = new AtomicReference<>();
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public FieldVisitor visitField(final int access,
                                           final String name,
                                           final String desc,
                                           final String signature,
                                           final Object value) {
                if ("serialVersionUID".equals(name) && Type.getType(desc).getSort() == Type.LONG) {
                    result.set((Long) value);
                }

                return super.visitField(access, name, desc, signature, value);
            }
        }, ClassReader.SKIP_FRAMES & ClassReader.SKIP_CODE & ClassReader.SKIP_FRAMES);

        final Long value = result.get();
        if (value == null) {
            throw new IllegalStateException();
        }

        return value;
    }

    private static long getSerialVersionUID(final Class<?> clazz) throws NoSuchFieldException, IllegalAccessException {
        final Field field = clazz.getDeclaredField("serialVersionUID");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                field.setAccessible(true);
                return null;
            }
        });

        return field.getLong(clazz);
    }

}
