package org.nohope.compiler;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.singletonMap;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-15 16:45
 */
@SuppressWarnings({"resource", "HardcodedFileSeparator"})
public class CompilerTest {

    @Test
    public void dependencyEnumeration() throws IOException {
        final String fdqn = "com.test.A";
        final StringBuilder src1 = new StringBuilder();
        src1.append("package com.test;");
        src1.append("import java.util.Map;");
        src1.append("import java.nio.ByteBuffer;");
        src1.append("public class A {");
        src1.append("  private int p0;");
        src1.append("  private Integer p1;");
        src1.append("  @Deprecated private Map<String[], ? extends Map<String, ? extends CharSequence[]>[]> p2;");
        src1.append("  private Long[] p3;");
        src1.append("  private class B {");
        src1.append("    private ByteBuffer p0;");
        src1.append("  };");
        src1.append("  private void method() {};");
        src1.append('}');

        final ClassFileManager manager = compileCode(singletonMap(fdqn, src1));

        final byte[] bytecodeA = manager.getCompiledCode().get("com.test.A");

        final ClassReader classReader = new ClassReader(bytecodeA);

        final DependencyVisitor classVisitor = new DependencyVisitor();
        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG & ClassReader.SKIP_CODE);

        System.err.println(classVisitor.getDescriptors());

        final ClassReader reader = new ClassReader(bytecodeA);
        final Set<String> classes = new HashSet<>();
        final Remapper remapper = new Collector(classes, "");
        final ClassVisitor inner = new ClassWriter(reader, 0);
        final RemappingClassAdapter visitor = new RemappingClassAdapter(inner, remapper);
        reader.accept(visitor, 0);

        System.err.println(classes);
    }


    private static ClassFileManager compileCode(final Map<String, ? extends CharSequence> sources) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final JavaFileManager fileManager = CompilerUtils.getFileManager()) {
            final Collection<JavaFileObject> files = new ArrayList<>();
            for (final Map.Entry<String, ? extends CharSequence> entry : sources.entrySet()) {
                files.add(CompilerUtils.sourceToFileObject(entry.getKey(), entry.getValue()));
            }
            compiler.getTask(null, fileManager, null, null, null, files).call();
            return (ClassFileManager) fileManager;
        }
    }
}
