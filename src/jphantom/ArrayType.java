package jphantom;

import org.objectweb.asm.Type;

public class ArrayType
{
    private ArrayType() {
        throw new AssertionError();
    }

    public static Type fromElementType(Type elementType, int dimensions)
    {
        if (dimensions < 1)
            throw new IllegalArgumentException("Non-positive dimension argument: " + dimensions);

        StringBuilder builder = new StringBuilder();

        // Append array descriptor prefix
        for (int i = 0; i < dimensions; i++)
            builder.append("[");

        // Append Element type
        builder.append(elementType.getDescriptor());

        return Type.getObjectType("" + builder);
    }

    public static Type fromElementType(Type elementType) {
        return fromElementType(elementType, 1);
    }

    public static Type fromType(Type type) {
        if (type.getSort() != Type.ARRAY)
            throw new IllegalArgumentException("Non-array type: " + type);
        return fromElementType(type.getElementType(), type.getDimensions());
    }

    public static Type elementOf(Type type)
    {
        int dim = fromType(type).getDimensions();
        Type basic = type.getElementType();

        return (dim-- == 1) ? basic : fromElementType(basic, dim);
    }
}
