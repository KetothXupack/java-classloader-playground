package org.nohope.bytecode.migration;

import com.test.versioned.FinalEntity;
import com.test.versioned.migration.FromV1ToV2;
import com.test.versioned.migration.FromV2ToV3;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nohope.bytecode.ClassLoaderUtils;
import org.nohope.compiler.ClassFileManager;
import org.nohope.serialization.JavaProvider;
import org.nohope.serialization.SerializationProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.nohope.compiler.CompilerTestUtils.compileCode;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 05:22
 */
@SuppressWarnings({"resource", "HardcodedFileSeparator"})
public class MigrationTest {
    @Test
    public void migrationTest() throws Exception {
        final int value = ThreadLocalRandom.current().nextInt();

        final ClassFileManager v1manager =
                compileCode("com.test.versioned.foo.Entity",
                            getResource("migration/version1/Entity.java"));

        final Map<String, String> v2sources = new HashMap<>();
        v2sources.put("com.test.versioned.Entity2", getResource("migration/version2/Entity2.java"));
        v2sources.put("com.test.versioned.SubEntity", getResource("migration/version2/SubEntity.java"));
        final ClassFileManager v2manager = compileCode(v2sources);

        final InMemoryBytecodeRepository repository = new InMemoryBytecodeRepository();

        // loading set of migrations
        repository.putMigrations(new FromV1ToV2(), new FromV2ToV3());

        // loading bytecode of all previous versions
        repository.putBytecode("9df299e6-f594-4bb0-8a8a-8a03f44ba32f", 1, v1manager.getCompiledCode().values());
        repository.putBytecode("9df299e6-f594-4bb0-8a8a-8a03f44ba32f", 2, v2manager.getCompiledCode().values());

        // latest bytecode version not needed
        //repository.putBytecode("9df299e6-f594-4bb0-8a8a-8a03f44ba32f", 3, Arrays.asList(
        //        ByteCodeUtils.getBytecode(FinalEntity.class),
        //        ByteCodeUtils.getBytecode(FinalSubEntity.class)
        //));

        final SerializationProvider migrator = new Migrator(new JavaProvider(), repository);

        final byte[] v1Serialized;
        {
            // getting serialized version of old object
            final ClassLoader v1ClassLoader = v1manager.getClassLoader(null);
            final Class<?> v1Class = v1ClassLoader.loadClass("com.test.versioned.foo.Entity");
            final Constructor<?> constructor = v1Class.getConstructor(int.class);
            final Serializable v1 = (Serializable) constructor.newInstance(value);
            v1Serialized = ClassLoaderUtils.runUsingClassLoader(v1ClassLoader, new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        migrator.writeObject(out, v1);
                        return out.toByteArray();
                    }
                }
            });
        }

        final FinalEntity v3 = migrator.readObject(new ByteArrayInputStream(v1Serialized), FinalEntity.class);
        assertEquals(Integer.toString(value), v3.getEntity().getValue());
    }

    private static String getResource(final String path) throws IOException {
        return IOUtils.toString(ClassLoader.getSystemResourceAsStream(path));
    }
}
