package jphantom.adapters;

import java.util.*;
import java.io.IOException;
import jphantom.tree.*;
import jphantom.*;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomAdder extends SignatureVisitor implements Opcodes
{
    private final ClassHierarchy hierarchy;
    private final ClassMembers members;
    private final Phantoms phantoms;

    private final static Logger logger = 
        LoggerFactory.getLogger(PhantomAdder.class);

    public PhantomAdder(ClassHierarchy hierarchy, ClassMembers members, Phantoms phantoms)
    {
        super(ASM4);
        this.hierarchy = hierarchy;
        this.members = members;
        this.phantoms = phantoms;
    }

    @Override
    public void visitInnerClassType(String name) {
        visitClassType(name);
    }

    @Override
    public void visitClassType(String name) {
        visitClassType(Type.getObjectType(name));
    }

    public void visitClassType(Type objType)
    {
        do {
            assert objType.getSort() == Type.OBJECT;

            /* Check if class already exists in the repository */
            if (hierarchy.contains(objType))
                break;

            /* Check if class can be resolved in the system (e.g. java.util) */
            try {
                // Use the bootstrap loader, so that it works for types
                // that happen to be included in jphantom itself

                Class<?> clazz = Class.forName(objType.getClassName(), false, null);

                Set<Type> prev = new HashSet<>();

                for (Type t : hierarchy)
                    prev.add(t);

                // Import from default class loader
                ClassHierarchies.loadSystemType(hierarchy, clazz);

                // Read members of library type
                for (Type t : hierarchy)
                    if (!prev.contains(t))
                        new ClassReader(objType.getInternalName()).accept(members.new Feeder(), 0);

                break;
            } catch (ClassNotFoundException ign) {
            } catch (IOException exc) {
                logger.warn("Could not locate library type: {}", objType);
                throw new RuntimeException(exc);
            }

            /* Add to phantom classes */
            if (!phantoms.contains(objType))
            {
                // Lazy implementation will provide a default value
                phantoms.getTransformer(objType);

                logger.info("Phantom Class \"{}\" detected", objType.getClassName());
            }
        } while (false);
    }
}
