package jphantom.conversions;

import org.objectweb.asm.Type;

public class WideningPrimitiveConversion extends PrimitiveConversion
{
    WideningPrimitiveConversion(Type from, Type to) {
        super(from, to);
    }

    @Override
    public void accept(ConversionVisitor visitor) {
        visitor.visit(this);
    }
}
