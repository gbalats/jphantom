package org.clyze.jphantom.conversions;

import org.objectweb.asm.Type;

public abstract class PrimitiveConversion extends Conversion
{
    PrimitiveConversion(Type from, Type to) {
        super(from, to);

        if (!Conversions.isPrimitive(from))
            throw new IllegalArgumentException("" + from);

        if (!Conversions.isPrimitive(to))
            throw new IllegalArgumentException("" + to);
    }
}
