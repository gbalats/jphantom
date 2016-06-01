package jphantom;

import java.util.*;
import org.objectweb.asm.Type;

/**
 * Constant interface (anti)pattern for some commonly used ASM
 * types.
 */
public interface Types
{
    /** The ASM type of {@link java.lang.Object}. */
    Type OBJECT = Type.getType(Object.class);


    /** The ASM type of {@link java.lang.Throwable}. */
    Type THROWABLE = Type.getType(Throwable.class);


    /** The ASM null type. This is the type of the expression null. */
    Type NULL_TYPE = Type.getObjectType("null");


    /**
     * The set of interfaces implemented by arrays, as ASM types.
     *
     * It consists of the {@link java.lang.Cloneable} and {@link
     * java.io.Serializable} interfaces.
     */
    Set<Type> ARRAY_INTERFACES =
        new HashSet<>(Arrays.asList(Type.getType(Cloneable.class),
                                    Type.getType(java.io.Serializable.class)));
}
