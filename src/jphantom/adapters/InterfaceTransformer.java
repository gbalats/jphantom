package jphantom.adapters;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;

public class InterfaceTransformer extends ClassVisitor implements Opcodes
{
    public InterfaceTransformer(ClassVisitor cv) {
        super(ASM5, cv);
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
