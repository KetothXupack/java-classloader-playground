package org.nohope.compiler;

import org.nohope.bytecode.core.HidingClassLoader;

import javax.tools.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 20:16
 */
public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    /**
     * Storage for compiled bytecode of our classes
     */
    private final Map<String, JavaClassObject> lookup = new HashMap<>();
    private final ClassLoader parentClassLoader;

    /**
     * Will initialize the manager with the specified
     * standard java file manager
     */
    ClassFileManager(final StandardJavaFileManager standardManager,
                     final ClassLoader parentClassLoader) {
        super(standardManager);
        this.parentClassLoader = parentClassLoader;
    }

    ClassFileManager(final StandardJavaFileManager standardManager) {
        this(standardManager, ClassLoader.getSystemClassLoader());
    }

    /**
     * Will be used by us to get the class loader for our
     * compiled class. It creates an anonymous class
     * extending the SecureClassLoader which uses the
     * byte code created by the compiler and stored in
     * the JavaClassObject, and returns the Class for it
     */
    @Override
    public HidingClassLoader getClassLoader(final Location location) {
        return HidingClassLoader.newBuilder(parentClassLoader)
                .addByteCode(getCompiledCode().values())
                .build();
    }

    public Map<String, byte[]> getCompiledCode() {
        final Map<String, byte[]> result = new HashMap<>();
        for (final Map.Entry<String, JavaClassObject> e : lookup.entrySet()) {
            result.put(e.getKey(), e.getValue().getBytes());
        }
        return result;
    }

    /**
     * Gives the compiler an instance of the JavaClassObject
     * so that the compiler can write the byte code into it.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
                                               final String className,
                                               final JavaFileObject.Kind kind,
                                               final FileObject sibling) throws IOException {
        final JavaClassObject object = new JavaClassObject(className, kind);
        lookup.put(className, object);
        return object;
    }
}
