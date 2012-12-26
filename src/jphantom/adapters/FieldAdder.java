package jphantom.adapters;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

public class FieldAdder extends ClassVisitor implements Opcodes
{
    private final FieldNode fn;
    private boolean isFieldPresent;

    public FieldAdder(ClassVisitor cv, FieldNode fn)
    {
        super(ASM4, cv);
        this.fn = fn;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value)
    {
        if (name.equals(fn.name))
            isFieldPresent = true;
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        if (!isFieldPresent)
            fn.accept(cv);
        super.visitEnd();
    }
}
