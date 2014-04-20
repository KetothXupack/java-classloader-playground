package org.nohope.bytecode;

import groovyjarjarasm.asm.Type;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-19 22:37
 */
public class ByteCodeUtilsTest {

    @Test
    public void className() throws IOException {
        final byte[] bytes = ByteCodeUtils.getBytecode(ByteCodeUtils.class);
        assertNotNull(bytes);

        assertEquals(ByteCodeUtils.class.getCanonicalName(),
                     ByteCodeUtils.getCanonicalName(bytes));
        assertEquals(Type.getInternalName(ByteCodeUtils.class),
                     ByteCodeUtils.getInternalName(bytes));
    }
}
