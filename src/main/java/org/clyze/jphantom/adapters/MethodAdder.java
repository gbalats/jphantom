package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.clyze.jphantom.methods.MethodSignature;

public class MethodAdder extends ClassVisitor implements Opcodes
{
    private final int mAcc;
    private final String mName;
    private final String mDesc;
    private final String[] mExc;
    private boolean isMethodPresent;
    private boolean iface;

    public MethodAdder(ClassVisitor cv, MethodSignature m) {
        this(cv, m.getAccess(), m.getName(), m.getDescriptor(),
             m.getExceptionNames());
    }

    public MethodAdder(ClassVisitor cv, int mAcc, String mName, String mDesc) {
        this(cv, mAcc, mName, mDesc, null);
    }

    public MethodAdder(
        ClassVisitor cv, int mAcc, String mName, String mDesc, String[] mExc)
    {
        super(Options.ASM_VER, cv);
        this.mAcc = mAcc;
        this.mName = mName;
        this.mDesc = mDesc;
        this.mExc = mExc;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, 
                                     String signature, String[] exceptions)
    {
        if (name.equals(mName) && desc.equals(mDesc))
            isMethodPresent = true;
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visit(int version, int access, String name, 
                      String signature, String superName, String[] interfaces)
    {
        iface = (access & ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        if (!isMethodPresent && cv != null)
        {
            boolean isStatic = (mAcc & ACC_STATIC) != 0;
            boolean isAbstract = (mAcc & ACC_ABSTRACT) != 0;

            MethodVisitor mv = cv.visitMethod(
                mAcc, mName, mDesc, null, mExc);

            if (mv != null) {
                // Add method body (only for classes or static methods)

                if (isStatic || (!iface && !isAbstract))
                {
                    int maxStack = 2;
                    int maxLocals = 0;

                    for (Type arg : Type.getArgumentTypes(mDesc))
                        maxLocals += arg.getSize();

                    mv.visitCode();

                    if (!isStatic) {
                        mv.visitVarInsn(ALOAD, 0); // this
                        maxStack++;
                        maxLocals++;
                    }

                    mv.visitInsn(NOP);

                    String exc = Type.getInternalName(
                        UnsupportedOperationException.class);

                    String desc = Type.getMethodType(Type.VOID_TYPE)
                        .getDescriptor();

                    mv.visitTypeInsn(NEW, exc);
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL, exc, "<init>", desc, iface);
                    mv.visitInsn(ATHROW);
                    mv.visitMaxs(maxStack, maxLocals);
                }
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }
}
