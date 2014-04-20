package org.nohope.bytecode;

import org.nohope.bytecode.core.HidingClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 06:00
 */
public final class ClassLoaderUtils {
    private ClassLoaderUtils() {
    }

    public static ClassLoader currentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static <T> T runUsingClassLoader(final ClassLoader classLoader,
                                            final Callable<T> callable) throws InvocationTargetException {
        final AtomicReference<T> result = new AtomicReference<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(callable.call());
                } catch (final Exception e) {
                    exception.set(e);
                }
            }
        });

        thread.setContextClassLoader(classLoader);

        thread.start();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            throw new InvocationTargetException(e);
        }

        final Throwable err = exception.get();
        if (err != null) {
            throw new InvocationTargetException(err);
        }

        return result.get();
    }

    public static Class<?> loadClass(final String name) throws ClassNotFoundException {
        return currentClassLoader().loadClass(name);
    }

    public static HidingClassLoader classLoader(final Iterable<byte[]> byteCode) {
        return HidingClassLoader.newBuilder().addByteCode(byteCode).build();
    }
}
