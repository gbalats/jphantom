package jphantom.conversions;

import java.util.*;
import jphantom.*;
import util.*;
import org.objectweb.asm.Type;

import static util.Utils.*;

public class Conversions implements StandardTypes {

    private Conversions() {
        throw new AssertionError();
    }

    // Primitive Conversion Maps

    private static final Map<Type, Set<Type>> WIDENING_PRIMITIVE_CONVERSIONS =
        newMap();

    private static final Map<Type, Set<Type>> NARROWING_PRIMITIVE_CONVERSIONS =
        newMap();

    public static boolean isPrimitive(Type t)
    {
        switch (t.getSort()) {
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.DOUBLE:
        case Type.FLOAT:
        case Type.INT:
        case Type.SHORT:
        case Type.LONG:
            return true;
        case Type.METHOD:
        case Type.OBJECT:
        case Type.VOID:
        case Type.ARRAY:
            return false;
        default:
            throw new AssertionError();
        }
    }

    public static boolean isReference(Type t)
    {
        switch (t.getSort()) {
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.DOUBLE:
        case Type.FLOAT:
        case Type.INT:
        case Type.SHORT:
        case Type.LONG:
        case Type.METHOD:
        case Type.VOID:
            return false;
        case Type.OBJECT:
        case Type.ARRAY:
            return true;
        default:
            throw new AssertionError();
        }
    }

    static {
        Set<Type> tmp = newSet(Arrays.asList(Type.DOUBLE_TYPE));

        Type[] types = {
            Type.FLOAT_TYPE, Type.LONG_TYPE, Type.INT_TYPE, 
            Type.CHAR_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE };

        for (Type t : types) {
            WIDENING_PRIMITIVE_CONVERSIONS.put(t, newSet(tmp));
            tmp.add(t);
        }

        NARROWING_PRIMITIVE_CONVERSIONS.put(
            Type.INT_TYPE, newSet(
                Arrays.asList(Type.CHAR_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE, Type.BOOLEAN_TYPE)));
    }

    private static final boolean isWideningPrimConv(Pair<Type,Type> pair) {
        return WIDENING_PRIMITIVE_CONVERSIONS.containsKey(pair.fst) && 
            WIDENING_PRIMITIVE_CONVERSIONS.get(pair.fst).contains(pair.snd);
    }

    private static final boolean isNarrowingPrimConv(Pair<Type,Type> pair) {
        return NARROWING_PRIMITIVE_CONVERSIONS.containsKey(pair.fst) && 
            NARROWING_PRIMITIVE_CONVERSIONS.get(pair.fst).contains(pair.snd);
    }

    // Cache

    private static final Map<Pair<Type,Type>,Conversion> cache = newMap();

    public static Conversion getAssignmentConversion(Type from, Type to)
    {
        Pair<Type,Type> pair = new Pair<Type,Type>(from, to);

        if (!cache.containsKey(pair))
            cache.put(pair, newAssignmentConversion(pair));

        return cache.get(pair);
    }

    public static Conversion newAssignmentConversion(Pair<Type,Type> pair)
    {
        Type from = pair.fst;
        Type to = pair.snd;

        // Identity conversion
        if (from.equals(to))
            return new IdentityConversion(from, to);

        // Widening primitive conversion
        if (isWideningPrimConv(pair))
            return new WideningPrimitiveConversion(from, to);

        // Narrowing primitive conversion
        if (isNarrowingPrimConv(pair))
            return new NarrowingPrimitiveConversion(from, to);

        // Non-reference types

        if (!isReference(from))
            return new IllegalConversion(from, to);
        
        if (!isReference(to))
            return new IllegalConversion(from, to);

        // Widening reference conversion

        if (from.equals(NULL_TYPE))
            return new NullConversion(to);

        // Array-type conversions

        if (from.getSort() == Type.ARRAY) {
            do {
                // Array implemented interfaces
                if (ARRAY_INTERFACES.contains(to))
                    break;

                // Array supertype
                if (to.equals(OBJECT))
                    break;

                if (to.getSort() == Type.ARRAY)
                {
                    Conversion subconv = getAssignmentConversion(
                        ArrayType.elementOf(from),
                        ArrayType.elementOf(to));

                    if (!(subconv instanceof IllegalConversion))
                        break;
                }
                return new IllegalConversion(from, to);
            } while(false);

        } else if (to.getSort() == Type.ARRAY) {
            return new IllegalConversion(from, to);
        }
        
        return new WideningReferenceConversion(from, to);
    }
}
