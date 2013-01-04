package jphantom.adapters;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import jphantom.access.Modifier;
import jphantom.exc.IllegalBytecodeException;
import static jphantom.access.Modifier.*;

public class MethodAdder extends ClassVisitor implements Opcodes
{
    private final MethodNode mn;
    private boolean isMethodPresent;
    private boolean iface;

    public MethodAdder(ClassVisitor cv, MethodNode mn)
    {
        super(ASM4, cv);
        this.mn = mn;
        // mn.visitMaxs(0, 0);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, 
                                     String signature, String[] exceptions)
    {
        if (name.equals(mn.name) && desc.equals(mn.desc))
            isMethodPresent = true;
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visit(int version, int access, String name, 
                      String signature, String superName, String[] interfaces)
    {
        boolean iface;

        try {
            iface = decode(access).contains(INTERFACE);
        } catch (Modifier.IllegalModifierException exc) {
            throw new IllegalBytecodeException.Builder(name)
                .cause(exc).build();
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        if (!isMethodPresent)
        {
            boolean isStatic, isAbstract;
                
            try {
                isStatic = decode(mn.access).contains(STATIC);
                isAbstract = decode(mn.access).contains(ABSTRACT);
            } catch (Modifier.IllegalModifierException exc) {
                throw new AssertionError("Modifiers are already tested");
            }

            // Add method body (only for classes)
            if (!iface && !isAbstract)
            {
                int maxStack = 2;
                int maxLocals = 1 + Type.getArgumentTypes(mn.desc).length;

                mn.visitCode();

                if (!isStatic) {
                    mn.visitVarInsn(ALOAD, 0); // this
                    maxStack++;
                    maxLocals++;
                }

                mn.visitInsn(NOP);

                String exc = Type.getInternalName(
                    UnsupportedOperationException.class);

                String desc = Type.getMethodType(Type.VOID_TYPE)
                    .getDescriptor();

                mn.visitTypeInsn(NEW, exc);
                mn.visitInsn(DUP);
                mn.visitMethodInsn(INVOKESPECIAL, exc, "<init>", desc);
                mn.visitInsn(ATHROW);
                mn.visitMaxs(maxStack, maxLocals);
                mn.visitEnd();
            }
            mn.accept(cv);
        }
        super.visitEnd();
    }
}
