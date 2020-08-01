package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;

public class AccessAdapter extends ClassVisitor implements Opcodes
{
    private final int access;

    public AccessAdapter(ClassVisitor cv, int access) {
        super(Options.ASM_VER, cv);
        this.access = access;
    }

    @Override
    public void visit(int version, int oldAccess, 
        String name, String signature, 
        String superName, String[] interfaces)
    {
        super.visit(
            version, 
            this.access, 
            name, 
            signature, 
            superName, 
            interfaces);
    }
}
