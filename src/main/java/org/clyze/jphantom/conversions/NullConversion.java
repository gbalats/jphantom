package org.clyze.jphantom.conversions;

import org.objectweb.asm.Type;
import org.clyze.jphantom.Types;

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
