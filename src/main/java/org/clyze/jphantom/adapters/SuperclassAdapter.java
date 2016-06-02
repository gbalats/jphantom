package org.clyze.jphantom.adapters;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassVisitor;

public class SuperclassAdapter extends ClassVisitor implements Opcodes
{
    private final Type superclass;

    public SuperclassAdapter(ClassVisitor cv, Type superclass)
    {
        super(ASM5, cv);
        this.superclass = superclass;
    }

    @Override
    public void visit(
        int version, 
        int access, 
        String name, 
        String signature, 
        String superName, 
        String[] interfaces)
    {
        super.visit(
            version, 
            access, 
            name, 
            signature, 
            superclass.getInternalName(), 
            interfaces);
    }
}
