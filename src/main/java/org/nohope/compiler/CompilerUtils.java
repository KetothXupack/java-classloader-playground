package org.nohope.compiler;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 21:59
 */
public final class CompilerUtils {

    private CompilerUtils() {
    }

    public static JavaFileManager getFileManager() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        return new ClassFileManager(compiler.getStandardFileManager(null, null, null));
    }

    public static JavaFileObject sourceToFileObject(final String fqdn, final CharSequence source) {
        return new CharSequenceJavaFileObject(fqdn, source);
    }
}
