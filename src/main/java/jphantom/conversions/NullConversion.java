package jphantom.conversions;

import org.objectweb.asm.Type;
import jphantom.Types;

public class NullConversion extends WideningReferenceConversion
    implements Types
{
    NullConversion(Type to) {
        super(NULL_TYPE, to);
    }

    @Override
    public void accept(ConversionVisitor visitor) {
        visitor.visit(this);
    }
}
