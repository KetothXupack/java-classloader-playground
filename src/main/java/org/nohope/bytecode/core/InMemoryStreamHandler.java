package org.nohope.bytecode.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 01:14
 */
class InMemoryStreamHandler extends URLStreamHandler {
    private final InputStream stream;

    InMemoryStreamHandler(final InputStream stream) {
        this.stream = stream;
    }

    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
        return new URLConnection(u) {
            @Override
            public void connect() {
            }

            @Override
            public InputStream getInputStream() {
                return stream;
            }
        };
    }

}
