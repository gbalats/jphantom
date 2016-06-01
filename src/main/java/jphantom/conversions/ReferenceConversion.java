package jphantom.conversions;

import org.objectweb.asm.Type;

public abstract class ReferenceConversion extends Conversion
{
    ReferenceConversion(Type from, Type to) {
        super(from, to);

        if (!Conversions.isReference(from))
            throw new IllegalArgumentException("" + from);

        if (!Conversions.isReference(to))
            throw new IllegalArgumentException("" + to);
    }
}
