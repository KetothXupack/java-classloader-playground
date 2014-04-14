package org.nohope.compiler;

import javax.tools.*;
import java.io.IOException;
import java.security.SecureClassLoader;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 20:16
 */
class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    /**
     * Instance of JavaClassObject that will store the
     * compiled bytecode of our class
     */
    private JavaClassObject classObject;

    /**
     * Will initialize the manager with the specified
     * standard java file manager
     *
     * @param standardManager
     */
    ClassFileManager(final StandardJavaFileManager standardManager) {
        super(standardManager);
    }

    /**
     * Will be used by us to get the class loader for our
     * compiled class. It creates an anonymous class
     * extending the SecureClassLoader which uses the
     * byte code created by the compiler and stored in
     * the JavaClassObject, and returns the Class for it
     */
    @Override
    public ClassLoader getClassLoader(final Location location) {
        return new SecureClassLoader(ClassLoader.getSystemClassLoader().getParent()) {

            @Override
            protected Class<?> findClass(final String name) {
                final byte[] b = classObject.getBytes();
                return super.defineClass(name, classObject.getBytes(), 0, b.length);
            }
        };
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
        classObject = new JavaClassObject(className, kind);
        return classObject;
    }
}
