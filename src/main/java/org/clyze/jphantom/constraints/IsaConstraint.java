package org.clyze.jphantom.constraints;

import org.objectweb.asm.Type;

public abstract class IsaConstraint implements Constraint
{
    public final Type type;

    public IsaConstraint(Type type)
    {
        if (type == null)
            throw new IllegalArgumentException();

        this.type = type;
    }
}
