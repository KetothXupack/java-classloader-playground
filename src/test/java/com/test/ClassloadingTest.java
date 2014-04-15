package com.test;

import org.junit.Test;
import org.nohope.compiler.CompilerUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 21:56
 */
public class ClassloadingTest {
    @Test
    public void conflicting() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, InterruptedException {
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

        final Class<?> clazz1 = compileCodeAndGetClass(fullName, src1);
        final Class<?> clazz2 = compileCodeAndGetClass(fullName, src2);

        final Object instance1 = clazz1.newInstance();
        final Object instance2 = clazz2.newInstance();

        assertEquals("TestClass.version1", new TestClass().toString());
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
        src1.append("public class A {");
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
        src2.append("public class B {");
        src2.append('}');

        sources.put("com.test.A", src1);
        sources.put("com.test.B", src2);

        final ClassLoader classLoader = compileCode(sources);

        final Class<?> classA = classLoader.loadClass("com.test.A");
        final Class<?> classB = classLoader.loadClass("com.test.B");

        final Constructor<?> constructorA = classA.getConstructor(classB);
        final Object instanceB = classB.newInstance();
        final Object instanceA = constructorA.newInstance(instanceB);

        final Method getterB = classA.getMethod("getB");

        assertSame(instanceB, getterB.invoke(instanceA));
    }

    private static ClassLoader compileCode(final Map<String, CharSequence> sources) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final JavaFileManager fileManager = CompilerUtils.getFileManager()) {
            final Collection<JavaFileObject> files = new ArrayList<>();
            for (final Map.Entry<String, CharSequence> entry : sources.entrySet()) {
                files.add(CompilerUtils.sourceToFileObject(entry.getKey(), entry.getValue()));
            }
            compiler.getTask(null, fileManager, null, null, null, files).call();
            return fileManager.getClassLoader(null);
        }
    }

    private static Class<?> compileCodeAndGetClass(final String fqdn, final CharSequence source)
            throws ClassNotFoundException, IOException {
        return compileCode(Collections.singletonMap(fqdn, source)).loadClass(fqdn);
    }
}
