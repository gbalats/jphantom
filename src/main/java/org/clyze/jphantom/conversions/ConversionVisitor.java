package org.clyze.jphantom.conversions;

public interface ConversionVisitor
{
    void visit(IdentityConversion conv);
    void visit(WideningPrimitiveConversion conv);
    void visit(WideningReferenceConversion conv);
    void visit(NarrowingPrimitiveConversion conv);
    void visit(IllegalConversion conv);
    void visit(NullConversion conv);
}
