package jphantom;

import org.objectweb.asm.Type;


/**
 * Static utility array-specific methods pertaining to
 * {@link org.objectweb.asm.Type} instances.
 */
public class ArrayTypes
{
    /** Should not be able to instantiate this class, whatsoever. */
    private ArrayTypes() {
        throw new AssertionError();
    }


    /**
     * Creates an array type instance, given a basic element type and
     * a dimension number.
     *
     * @param elementType  the basic element type
     * @param dimension    the dimension number
     * @return             a new array type
     */
    public static Type newType(Type elementType, int dimension)
    {
        // Array dimension should be positive
        if (dimension < 1)
            throw new IllegalArgumentException(
                "Non-positive dimension argument: " + dimension);

        StringBuilder builder = new StringBuilder();

        // Append array descriptor prefix
        for (int i = 0; i < dimension; i++)
            builder.append("[");

        // Append basic element type
        builder.append(elementType.getDescriptor());

        return Type.getObjectType("" + builder);
    }


    /**
     * Creates a one-dimensional array type instance, given a basic
     * element type.
     *
     * @param elementType  the basic element type
     * @return             a new array type
     */
    public static Type newType(Type elementType)
    {
        return newType(elementType, 1);
    }


    /**
     * Creates an array type instance, given another array type.
     *
     * Since {@link org.objectweb.asm.Type} is immutable, this method
     * is only useful for checking if its argument is indeed an array
     * type.
     *
     * @param arrayType    an array type
     * @return             a new array type
     *
     * @throws IllegalArgumentException  if given a non-array type
     * argument
     */
    public static Type checkedArrayType(Type arrayType)
    {
        if (arrayType.getSort() != Type.ARRAY)
            throw new IllegalArgumentException("Non-array type: " + arrayType);

        Type type = newType(arrayType.getElementType(), arrayType.getDimensions());

        // Sanity check
        assert arrayType.equals(type);

        return type;
    }


    /**
     * Returns the element type of an array type.
     *
     * For an {@code n}-dimensional array type, this method will
     * return a {@code (n-1)}-dimensional array type.
     *
     * @param arrayType    an array type
     * @return             the element type of {@code arrayType}
     */
    public static Type elementOf(Type arrayType)
    {
        int dim = checkedArrayType(arrayType).getDimensions();
        Type basic = arrayType.getElementType();

        return (dim-- == 1) ? basic : newType(basic, dim);
    }
}
