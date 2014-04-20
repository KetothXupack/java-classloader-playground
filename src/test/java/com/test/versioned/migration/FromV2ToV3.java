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
public class FromV2ToV3 extends AbstractMigration {
    public FromV2ToV3() {
        super("9df299e6-f594-4bb0-8a8a-8a03f44ba32f", 2, 3);
    }

    @Override
    public Serializable migrate(final Serializable source) throws Exception {
        final Field oldSubEntity = source.getClass().getDeclaredField("value");
        oldSubEntity.setAccessible(true);

        final Object source2 = oldSubEntity.get(source);
        final Field oldValue = source2.getClass().getDeclaredField("value");
        oldValue.setAccessible(true);

        final Class<?> finalEntityClass = ClassLoaderUtils.loadClass("com.test.versioned.FinalEntity");
        final Class<?> finalSubEntityClass = ClassLoaderUtils.loadClass("com.test.versioned.FinalSubEntity");

        final Constructor<?> finalSubEntityClassConstructor =
                finalSubEntityClass.getConstructor(String.class);

        final Object finalSubEntity =
                finalSubEntityClassConstructor.newInstance(Integer.toString(oldValue.getInt(source2)));

        final Constructor<?> finalEntityConstructor = finalEntityClass.getConstructor(finalSubEntityClass);
        return (Serializable) finalEntityConstructor.newInstance(finalSubEntity);
    }
}
