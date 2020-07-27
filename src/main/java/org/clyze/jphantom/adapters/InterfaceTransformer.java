package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;

public class InterfaceTransformer extends ClassVisitor implements Opcodes
{
    public InterfaceTransformer(ClassVisitor cv) {
        super(Options.ASM_VER, cv);
    }

    @Override
    public void visit(int version, int oldAccess, 
        String name, String signature, 
        String superName, String[] interfaces)
    {
        int access = oldAccess | ACC_INTERFACE;

        super.visit(
            version, 
            access, 
            name, 
            signature, 
            superName, 
            interfaces);
    }

    // TODO: remove method bodies
}
