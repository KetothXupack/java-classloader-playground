package com.test.versioned.migration;

import org.nohope.bytecode.ClassLoaderUtils;
import org.nohope.bytecode.migration.AbstractMigration;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-18 02:43
 */
public class FromV1ToV2 extends AbstractMigration {
    public FromV1ToV2() {
        super("9df299e6-f594-4bb0-8a8a-8a03f44ba32f", 1, 2);
    }

    @Override
    public Serializable migrate(final Serializable source) throws Exception {
        final Field value = source.getClass().getDeclaredField("value");
        value.setAccessible(true);

        final Class<?> clazz = ClassLoaderUtils.loadClass("com.test.versioned.Entity2");
        final Class<?> clazz2 = ClassLoaderUtils.loadClass("com.test.versioned.SubEntity");


        final Constructor<?> constructor2 = clazz2.getConstructor(int.class);
        final Object entity2 = constructor2.newInstance(value.getInt(source));

        final Constructor<?> constructor = clazz.getConstructor(clazz2);
        return (Serializable) constructor.newInstance(entity2);
    }
}
