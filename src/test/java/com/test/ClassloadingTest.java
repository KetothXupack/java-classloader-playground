package com.test;

import org.junit.Test;
import org.nohope.compiler.CompilerUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

        final Object instance1 = compileClassAndGetInstance(fullName, src1);
        final Object instance2 = compileClassAndGetInstance(fullName, src2);

        assertEquals("TestClass.version1", new TestClass().toString());
        assertEquals("TestClass.version2", instance1.toString());
        assertEquals("TestClass.version3", instance2.toString());
        assertNotEquals(TestClass.class, instance1.getClass());
        assertNotEquals(TestClass.class, instance2.getClass());
        assertNotEquals(instance1.getClass(), instance2.getClass());
    }

    private static Object compileClassAndGetInstance(final String fqdn, final StringBuilder source)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final JavaFileManager fileManager = CompilerUtils.getFileManager()) {
            final List<JavaFileObject> jfiles = new ArrayList<>();
            jfiles.add(CompilerUtils.sourceToFileObject(fqdn, source));
            compiler.getTask(null, fileManager, null, null, null, jfiles).call();
            return fileManager.getClassLoader(null).loadClass(fqdn).newInstance();
        }
    }
}
