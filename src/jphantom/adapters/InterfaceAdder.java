package jphantom.adapters;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassVisitor;

public class InterfaceAdder extends ClassVisitor implements Opcodes
{
    private final Set<String> ifaces;

    public InterfaceAdder(ClassVisitor cv, Set<Type> newInterfaces)
    {
        super(ASM4, cv);
        this.ifaces = new HashSet<String>();

        for (Type i : newInterfaces)
            ifaces.add(i.getInternalName());
    }

    @Override
    public void visit(int version, int access, 
        String name, String signature, 
        String superName, String[] interfaces)
    {
        Set<String> allIfaces = new HashSet<String>(ifaces);
        allIfaces.addAll(Arrays.asList(interfaces));
        super.visit(
            version, 
            access, 
            name, 
            signature, 
            superName, 
            ifaces.toArray(new String[0]));
    }
}
