package jphantom.conversions;

import org.objectweb.asm.Type;

public class IdentityConversion extends Conversion
{
    IdentityConversion(Type from, Type to) {
        super(from, to);
        if (!from.equals(to))
            throw new IllegalArgumentException(from + " != " + to);
    }

    @Override
    public void accept(ConversionVisitor visitor) {
        visitor.visit(this);
    }
}
