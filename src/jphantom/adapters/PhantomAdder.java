package jphantom.adapters;

import jphantom.tree.*;
import jphantom.Phantoms;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomAdder extends SignatureVisitor implements Opcodes
{
    private final ClassHierarchy hierarchy;
    private final Phantoms phantoms;

    private final static Logger logger = 
        LoggerFactory.getLogger(PhantomAdder.class);

    public PhantomAdder(ClassHierarchy hierarchy, Phantoms phantoms)
    {
        super(ASM4);
        this.hierarchy = hierarchy;
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
                Class<?> clazz = Class.forName(objType.getClassName());

                // Import from default class loader
                ClassHierarchies.loadSystemType(hierarchy, clazz);

                break;
            } catch (ClassNotFoundException ign) {}

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
