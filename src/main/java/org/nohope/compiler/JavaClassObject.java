package org.nohope.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-14 20:17
 */
public class JavaClassObject extends SimpleJavaFileObject {

    /**
     * Byte code created by the compiler will be stored in this
     * ByteArrayOutputStream so that we can later get the
     * byte array out of it
     * and put it in the memory as an instance of our class.
     */
    private final ByteArrayOutputStream bos;

    /**
     * Registers the compiled class object under URI
     * containing the class full name
     *
     * @param name
     *            Full name of the compiled class
     * @param kind
     *            Kind of the data. It will be CLASS in our case
     */
    JavaClassObject(final String name, final Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        bos = new ByteArrayOutputStream();
    }

    /**
     * Will be used by our file manager to get the byte code that
     * can be put into memory to instantiate our class
     *
     * @return compiled byte code
     */
    public byte[] getBytes() {
        return bos.toByteArray();
    }

    /**
     * Will provide the compiler with an output stream that leads
     * to our byte array. This way the compiler will write everything
     * into the byte array that we will instantiate later
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }
}
