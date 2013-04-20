package jphantom;

import java.util.*;
import org.objectweb.asm.Type;

public interface Types {

    Type OBJECT = Type.getType(Object.class);
    Type THROWABLE = Type.getType(Throwable.class);
    Type NULL_TYPE = Type.getObjectType("null");

    // Array Interfaces

    Set<Type> ARRAY_INTERFACES = 
        new HashSet<>(Arrays.asList(Type.getType(Cloneable.class), 
                                    Type.getType(java.io.Serializable.class)));
}
