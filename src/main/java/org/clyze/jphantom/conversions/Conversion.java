package org.clyze.jphantom.conversions;

import org.objectweb.asm.Type;
import org.clyze.jphantom.constraints.*;

public abstract class Conversion
{
    public final Type from;
    public final Type to;

    Conversion(Type from, Type to) {
        this.from = from;
        this.to = to;
    }

    public Constraint asConstraint() {
        return SubtypeConstraint.factory.createEdge(from, to);
    }

    public abstract void accept(ConversionVisitor visitor);
}
