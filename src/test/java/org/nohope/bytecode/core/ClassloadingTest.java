package org.nohope.bytecode.core;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.test.TestClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.junit.Test;
import org.nohope.bytecode.ByteCodeUtils;
import org.nohope.bytecode.ClassLoaderUtils;
import org.nohope.bytecode.migration.TypedMigration;
import org.nohope.compiler.ClassFileManager;
import org.nohope.compiler.CompilerTestUtils;
import org.nohope.compiler.CompilerUtils;
import org.nohope.serialization.ThreadContextClassLoaderAwareObjectInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;

import static java.util.Map.Entry;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 21:56
 */
@SuppressWarnings({"unchecked", "resource", "ClassNewInstance", "HardcodedFileSeparator"})
public class ClassloadingTest {

    public static class Mock {
        private final Map<String, Map<String, String>> sources = new HashMap<>();

        public Mock(final Map<String, Map<String, String>> versions) {
            this.sources.putAll(versions);
        }

        public Map<String, Map<String, String>> getVersions() {
            return Collections.unmodifiableMap(sources);
        }

        public Map<String, String> getSources(final String version) {
            return Collections.unmodifiableMap(sources.get(version));
        }
    }

    private static Mock readMock(final String name) throws Exception {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource(name + ".mock.xml");
        if (resource == null) {
            throw new IllegalArgumentException(name);
        }


        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(resource.openStream());
        final NodeList versions = doc.getElementsByTagName("version");

        final Map<String, Map<String, String>> groups = new HashMap<>();
        for (int i = 0; i < versions.getLength(); i++) {
            final Element version = (Element) versions.item(i);
            final String id = version.getAttribute("id");
            final Map<String, String> sourceValues = new HashMap<>();

            final NodeList sources = version.getElementsByTagName("source");
            for (int j = 0; j < sources.getLength(); j++) {
                final Element source = (Element) sources.item(j);
                final String sourceName = source.getAttribute("name");
                final String sourceContent = source.getTextContent();
                sourceValues.put(sourceName, sourceContent);
            }

            groups.put(id, sourceValues);
        }

        return new Mock(groups);
    }

    @Test
    public void mockCompile() throws Exception {
        final Mock mock = readMock("general-migration");

        final ClassFileManager v1manager = CompilerTestUtils.compileCode(mock.getSources("version1"));
        final ClassFileManager v2manager = CompilerTestUtils.compileCode(mock.getSources("version2"));

        final HidingClassLoader v1classLoader = (HidingClassLoader) v1manager.getClassLoader(null);
        final HidingClassLoader v2classLoader = (HidingClassLoader) v2manager.getClassLoader(null);

        final Map<String, byte[]> v1renamedBytecode = renameTypes(
                v1classLoader.getClassesByteCode().values(),
                new Function<String, String>() {
                    @Override
                    public String apply(final String input) {
                        return "$/" + input;
                    }
                });

        // make sure we don't have intersects in class names
        assertTrue(Sets.intersection(
                Sets.newHashSet(Collections2.transform(
                        v1renamedBytecode.values(),
                        new Function<byte[], String>() {
                            @Override
                            public String apply(final byte[] input) {
                                return ByteCodeUtils.getCanonicalName(input);
                            }
                        })),
                        v2classLoader.getClassesByteCode().keySet()
                ).isEmpty()
        );

        final HidingClassLoader v1ModifiedClassLoader =
                ClassLoaderUtils.classLoader(v1renamedBytecode.values());

        final HidingClassLoader migrationClassLoader =
                HidingClassLoader.merge(v1ModifiedClassLoader, v2classLoader);

        final Serializable v1instance = (Serializable) new GroovyShell(v1classLoader).evaluate(new StringBuilder()
                .append("import com.test.A;")
                .append("import com.test.Example;")
                .append("return new Example(123, new A(\"test\"));")
                .toString());

        final Serializable v1instancePrepared = remapFromRenamed(
                v1instance, HidingClassLoader.merge(v1classLoader, v1ModifiedClassLoader),
                v1renamedBytecode);

        final StringBuilder script = new StringBuilder()
                .append("package com.test;")
                .append("import org.nohope.bytecode.migration.Migration;")
                .append("import org.nohope.bytecode.migration.TypedMigration;")
                .append("import com.test.C;")
                .append("import com.test.Example;")
                .append("import java.io.Serializable;")
                .append("public class Migration implements TypedMigration<$.com.test.Example> {")
                .append("  @Override")
                .append("  public Example migrate($.com.test.Example source) {")
                .append("    int p1 = source.getB().getParam();")
                .append("    String p2 = source.getA().getParam();")
                .append("    C c = new C(p1, p2);")
                .append("    return new Example(c);")
                .append("  }")
                .append('}');

        final Class<?> migrationClass =
                new GroovyClassLoader(migrationClassLoader).parseClass(script.toString());
        final TypedMigration<Serializable> migration =
                (TypedMigration<Serializable>) migrationClass.newInstance();
        final Serializable v2instance = migration.migrate(v1instancePrepared);
        // TODO: remap to $ version, and continue...
    }

    @Test
    public void conflicting() throws Exception {
        final String fullName = "com.test.TestClass";

        final StringBuilder src1 = new StringBuilder();
        src1.append("package com.test;");
        src1.append("public class TestClass {");
        src1.append("  public static final int VERSION = 2;");
        src1.append("  public String toString() {");
        src1.append("    return \"TestClass.version\" + VERSION;");
        src1.append("  }");
        src1.append('}');

        final StringBuilder src2 = new StringBuilder();
        src2.append("package com.test;");
        src2.append("public class TestClass {");
        src2.append("  public static final int VERSION = 3;");
        src2.append("  public String toString() {");
        src2.append("    return \"TestClass.version\" + VERSION;");
        src2.append("  }");
        src2.append('}');

        // pre-load TestClass to system classloader
        final TestClass conflictingClazz = new TestClass();
        final Class<?> clazz1 = compileCodeAndGetClass(fullName, src1);
        final Class<?> clazz2 = compileCodeAndGetClass(fullName, src2);

        final Object instance1 = clazz1.newInstance();
        final Object instance2 = clazz2.newInstance();

        assertEquals("TestClass.version1", conflictingClazz.toString());
        assertEquals("TestClass.version2", instance1.toString());
        assertEquals("TestClass.version3", instance2.toString());
        assertNotEquals(TestClass.class, instance1.getClass());
        assertNotEquals(TestClass.class, instance2.getClass());
        assertNotEquals(instance1.getClass(), instance2.getClass());
    }

    @Test
    public void multipleFileCompilation() throws Exception {
        final Map<String, CharSequence> sources = new HashMap<>();

        final StringBuilder src1 = new StringBuilder();
        src1.append("package com.test;");
        src1.append("import java.io.Serializable;");
        src1.append("public class A implements Serializable {");
        src1.append("  private final B b;");
        src1.append("  public A(final B b) {");
        src1.append("    this.b = b;");
        src1.append("  }");
        src1.append("  public B getB() {");
        src1.append("    return this.b;");
        src1.append("  }");
        src1.append('}');

        final StringBuilder src2 = new StringBuilder();
        src2.append("package com.test;");
        src2.append("import java.io.Serializable;");
        src2.append("public class B implements Serializable {");
        src2.append('}');

        sources.put("com.test.A", src1);
        sources.put("com.test.B", src2);

        final ClassFileManager manager = CompilerTestUtils.compileCode(sources);

        final HidingClassLoader classLoader = (HidingClassLoader) manager.getClassLoader(null);

        final Class<?> classA = classLoader.loadClass("com.test.A");
        final Class<?> classB = classLoader.loadClass("com.test.B");

        final Constructor<?> constructorA = classA.getConstructor(classB);
        final Object instanceB = classB.newInstance();
        final Object instanceA = constructorA.newInstance(instanceB);

        final Method getterB = classA.getMethod("getB");
        assertSame(instanceB, getterB.invoke(instanceA));
    }

    private static Serializable remapFromRenamed(final Serializable object,
                                                 final ClassLoader classLoader,
                                                 final Map<String, byte[]> remapped)
            throws InvocationTargetException, ClassNotFoundException, IOException {

        final Map<String, Class<?>> substitutionMap = new HashMap<>();
        for (final Entry<String, byte[]> entry : remapped.entrySet()) {
            final String oldName = entry.getKey();
            final String newName = ByteCodeUtils.getCanonicalName(entry.getValue());
            substitutionMap.put(oldName, classLoader.loadClass(newName));
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final ObjectOutput renameStream = new RenamingObjectOutputStream(out, substitutionMap)) {
            renameStream.writeObject(object);
            return ClassLoaderUtils.runUsingClassLoader(classLoader, new Callable<Serializable>() {
                @Override
                public Serializable call() throws Exception {
                    final ByteArrayInputStream stream = new ByteArrayInputStream(out.toByteArray());
                    try(final ObjectInput objectInputStream = new ThreadContextClassLoaderAwareObjectInputStream(stream)) {
                        return (Serializable) objectInputStream.readObject();
                    }
                }
            });
        }
    }

    public static class Mapper implements Function<String, String> {
        private final Map<String, String> rules;

        public Mapper(final Map<String, String> rules) {
            this.rules = rules;
        }

        @Override
        public String apply(final String name) {
            return rules.get(name);
        }
    }

    private static Map<String, byte[]> renameTypes(final Iterable<byte[]> bytecode,
                                                   final Function<String, String> mapping) {
        final Map<String, String> rules = Maps.newHashMap();
        for (final byte[] bytes : bytecode) {
            final String internalName = ByteCodeUtils.getInternalName(bytes);
            rules.put(internalName, mapping.apply(internalName));
        }

        final Function<String, String> function = new Mapper(rules);
        final Map<String, byte[]> result = Maps.newHashMap();
        for (final byte[] bytes : bytecode) {
            final byte[] renamed = renameTypes(bytes, function);
            final String originalName = ByteCodeUtils.getCanonicalName(bytes);
            result.put(originalName, renamed);
        }

        return result;
    }

    private static byte[] renameTypes(final byte[] bytecode,
                                      final Function<String, String> function) {
        final ClassReader cr = new ClassReader(bytecode);

        final ClassWriter writer = new ClassWriter(Opcodes.ASM5);
        final RemappingClassAdapter adapter = new RemappingClassAdapter(writer, new Remapper() {
                @Override
                public String map(final String typeName) {
                    final String mappedName = function.apply(typeName);
                    if (mappedName != null) {
                        return mappedName;
                    }
                    return super.map(typeName);
                }
            });
        cr.accept(adapter, 0);
        return writer.toByteArray();
    }

    private static ClassLoader compile(final Map<String, CharSequence> sources) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final JavaFileManager fileManager = CompilerUtils.getFileManager()) {
            final Collection<JavaFileObject> files = new ArrayList<>();
            for (final Entry<String, CharSequence> entry : sources.entrySet()) {
                files.add(CompilerUtils.sourceToFileObject(entry.getKey(), entry.getValue()));
            }
            compiler.getTask(null, fileManager, null, null, null, files).call();
            return fileManager.getClassLoader(null);
        }
    }

    private static Class<?> compileCodeAndGetClass(final String fqdn, final CharSequence source)
            throws ClassNotFoundException, IOException {
        return compile(Collections.singletonMap(fqdn, source)).loadClass(fqdn);
    }
}
