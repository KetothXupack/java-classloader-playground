package org.nohope.compiler;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-15 16:39
 */
public class DependencyVisitor extends ClassVisitor {
    final Set<String> descriptors = new HashSet<>();

    public DependencyVisitor() {
        super(Opcodes.ASM5);
    }

    public static String getInternalName(final String description) {
        Type type = Type.getType(description);

        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }

        if (type.getSort() == Type.OBJECT) {
            return type.getInternalName();
        }

        return null;
    }

    private void addDescriptor(final String desc) {
        final String internalName = getInternalName(desc);
        if (internalName != null) {
            descriptors.add(internalName);
        }
    }

    @Override
    public void visitInnerClass(final String name,
                                final String outerName,
                                final String innerName,
                                final int access) {
        descriptors.add(name);
    }

    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String desc,
                                     final String signature,
                                     final String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM5) {
            @Override
            public void visitParameter(final String name, final int access) {
                super.visitParameter(name, access);
            }
        };
    }

    @Override
    public FieldVisitor visitField(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final Object value) {
        addDescriptor(desc);

        if (signature != null) {
            final SignatureReader signatureReader = new SignatureReader(signature);
            signatureReader.accept(new SignatureVisitor(Opcodes.ASM5) {
                @Override
                public void visitClassType(final String name) {
                    descriptors.add(name);
                }

                @Override
                public void visitTypeVariable(final String name) {
                    System.err.println(" --> " + name);
                    super.visitTypeVariable(name);
                }

                @Override
                public void visitInnerClassType(final String name) {
                    descriptors.add(name);
                }
            });
        }

        return new RegisteringFieldVisitor();
    }

    private class RegisteringFieldVisitor extends FieldVisitor {
        private RegisteringFieldVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                                     final TypePath typePath,
                                                     final String desc,
                                                     final boolean visible) {
            return new RegisteringAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            addDescriptor(desc);
            return new RegisteringAnnotationVisitor();
        }
    }

    private class RegisteringAnnotationVisitor extends AnnotationVisitor {
        private RegisteringAnnotationVisitor() {
            super(Opcodes.ASM5);
        }


        @Override
        public AnnotationVisitor visitAnnotation(final String name, final String desc) {
            return super.visitAnnotation(name, desc);
        }

        @Override
        public void visit(final String name, Object value) {
            System.err.println("---> " + name);
        }
    }

    public Set<String> getDescriptors() {
        return descriptors;
    }
}
