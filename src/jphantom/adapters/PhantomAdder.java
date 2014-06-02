package jphantom.adapters;

import java.util.*;
import java.io.IOException;
import jphantom.hier.*;
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
        super(ASM5);
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
        assert objType.getSort() == Type.OBJECT;

        /* Check if class already exists in the repository */
        if (hierarchy.contains(objType))
            return;

        /* Check if class can be resolved in the system (e.g. java.util) */
        try {
            // Use the bootstrap loader, so that it works for types
            // that happen to be included in jphantom itself

            Class<?> clazz = Class.forName(objType.getClassName(), false, null);

            // At this point, a library type was found. We first have
            // to add it to the hierarchy of referenced types, along
            // with all of its supertypes.

            // Create a set of types referenced so far.

            Set<Type> prev = new HashSet<>();

            for (Type t : hierarchy)
                prev.add(t);

            // Import from default class loader. At this point, we may
            // be at an inconsistent state since a library type could
            // have been added recursively to the hierarchy, but its
            // members may have not been recorded.

            ClassHierarchies.loadSystemType(hierarchy, clazz);

            assert hierarchy.contains(objType) && !prev.contains(objType);

            // Record members of each library type, added just now.
            for (Type t : hierarchy)
                if (!prev.contains(t))
                    new ClassReader(t.getInternalName()).accept(members.new Feeder(), 0);

            return;
        } catch (ClassNotFoundException ign) {

            // Class could not be loaded. Record it as a phantom class...
            // Note intentional fallthrough.

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
    }
}
