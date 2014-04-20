package org.nohope.compiler;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 05:23
 */
public final class CompilerTestUtils {
    private CompilerTestUtils() {
    }

    public static ClassFileManager compileCode(final String fqn, final CharSequence source) throws IOException {
        return compileCode(Collections.singletonMap(fqn, source));
    }

    public static ClassFileManager compileCode(final Map<String, ? extends CharSequence> sources) throws IOException {
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
