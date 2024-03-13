package org.clyze.jphantom.hier.closure;

import org.clyze.jphantom.hier.*;
import org.objectweb.asm.Type;

public abstract class AbstractSnapshot extends ForwardingClassHierarchy
    implements ClassHierarchy.Snapshot
{    
    public AbstractSnapshot(ClassHierarchy hierarchy) {
        super(hierarchy);
    }

    @Override
    public boolean isSubtypeOf(Type type, Type supertype) 
        throws IncompleteSupertypesException
    {
        if (!contains(type))
            throw new IllegalArgumentException("Snapshot does not contain type: " + type);

        return type.equals(supertype) || isStrictSubtypeOf(type, supertype);
    }

    @Override
    public boolean isStrictSubtypeOf(Type type, Type supertype)
        throws IncompleteSupertypesException
    {
        checkedContainedObject(type);

        try {
            return getAllSupertypes(type).contains(supertype);
        } catch (IncompleteSupertypesException exc) {
            if (exc.getSupertypes().contains(supertype))
                return true;
            throw new IncompleteSupertypesException();
        }
    }
}
