package jphantom.conversions;

import org.objectweb.asm.Type;

public class IllegalConversion extends Conversion
{
    IllegalConversion(Type from, Type to) {
        super(from, to);
    }

    @Override
    public void accept(ConversionVisitor visitor) {
        visitor.visit(this);
    }
}
