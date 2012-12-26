package jphantom.adapters;

import java.util.*;
import jphantom.access.Modifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassVisitor;
import static jphantom.access.Modifier.*;

public class InterfaceTransformer extends ClassVisitor implements Opcodes
{
    public InterfaceTransformer(ClassVisitor cv) {
        super(ASM4, cv);
    }

    @Override
    public void visit(int version, int oldAccess, 
        String name, String signature, 
        String superName, String[] interfaces)
    {
        Set<Modifier> modifiers = decode(oldAccess);
        modifiers.add(INTERFACE);

        super.visit(
            version, 
            encode(modifiers), 
            name, 
            signature, 
            superName, 
            interfaces);
    }

    // TODO: remove method bodies
}
