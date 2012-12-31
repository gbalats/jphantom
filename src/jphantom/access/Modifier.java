package jphantom.access;

import java.util.*;
import java.lang.annotation.ElementType;
import org.objectweb.asm.Opcodes;
import static java.lang.annotation.ElementType.*;

/**
 * A Java Modifier keyword for classes, fields, methods, etc.
 *
 * @author George Balatsouras <gbalats @ di.uoa.gr>
 */
public enum Modifier implements ApplicableToElements {
    ABSTRACT(Opcodes.ACC_ABSTRACT, TYPE, METHOD),
    ANNOTATION(Opcodes.ACC_ANNOTATION, TYPE),
    BRIDGE(Opcodes.ACC_BRIDGE, METHOD, CONSTRUCTOR),
    DEPRECATED(Opcodes.ACC_DEPRECATED),
    ENUM(Opcodes.ACC_ENUM, TYPE, FIELD),
    FINAL(Opcodes.ACC_FINAL, TYPE, FIELD, METHOD),
    INTERFACE(Opcodes.ACC_INTERFACE, TYPE),
    NATIVE(Opcodes.ACC_NATIVE, METHOD, CONSTRUCTOR),
    PRIVATE(Opcodes.ACC_PRIVATE),
    PROTECTED(Opcodes.ACC_PROTECTED),
    PUBLIC(Opcodes.ACC_PUBLIC),
    STATIC(Opcodes.ACC_STATIC, TYPE, FIELD, METHOD),
    STRICT(Opcodes.ACC_STRICT, "strictfp", TYPE, METHOD),
    SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED, METHOD),
    SYNTHETIC(Opcodes.ACC_SYNTHETIC),
    SUPER(Opcodes.ACC_SUPER, TYPE),
    TRANSIENT(Opcodes.ACC_TRANSIENT, FIELD),
    VARARGS(Opcodes.ACC_VARARGS, METHOD, CONSTRUCTOR),
    VOLATILE(Opcodes.ACC_VOLATILE, FIELD);
    
    private final int modifier;
    private final String repr;
    private final Set<ElementType> types;

    private Modifier(int modifier) {
        this(modifier, TYPE, METHOD, FIELD, CONSTRUCTOR);
    }

    private Modifier(int modifier, ElementType first, ElementType ... rest)
    {
        this(modifier, null, first, rest);
    }

    private Modifier(int modifier, String repr, ElementType first, ElementType ... rest)
    {
        this.modifier = modifier;
        this.repr = repr;
        this.types = EnumSet.of(first, rest);
    }

    /* Static fields */

    private static final Map<String, Modifier> stringToEnum =
        new HashMap<String, Modifier>();

    private static final ConflictDictionary confDict =
        new ConflictDictionary();    

    // A Symmetric Conflict Dictionary
    private static class ConflictDictionary {
        private final Map<Modifier, Set<Modifier>> dict =
            new EnumMap<Modifier, Set<Modifier>>(Modifier.class);

        ConflictDictionary() {
            for (Modifier m : Modifier.values())
                dict.put(m, new HashSet<Modifier>());
        }

        protected void addConflict(Modifier a, Modifier b) {
            // Guarantees symmetry
            dict.get(a).add(b);
            dict.get(b).add(a);
        }

        public Set<Modifier> conflictsOf(Modifier m) {
            return dict.get(m);
        }
    }

    static { // Initialize map from modifier name to enum constant
        for (Modifier m : values())
            stringToEnum.put(m.toString(), m);
       
        // Insert Modifier Conflicts
        
        confDict.addConflict(PRIVATE, PROTECTED);
        confDict.addConflict(PRIVATE, PUBLIC);
        confDict.addConflict(PROTECTED, PUBLIC);

        confDict.addConflict(ABSTRACT, PRIVATE);
        confDict.addConflict(ABSTRACT, FINAL);
        confDict.addConflict(ABSTRACT, STATIC);
        confDict.addConflict(ABSTRACT, SYNCHRONIZED);

        confDict.addConflict(FINAL, VOLATILE);

        confDict.addConflict(INTERFACE, FINAL);
        confDict.addConflict(INTERFACE, ENUM);
    }


    /**
     * Returns the unmodifiable set of conflicting modifiers for this enum constant.
     *
     * @return the unmodifiable set of conflicting modifiers for this enum constant
     */
    public Set<Modifier> conflictSet() {
        return Collections.unmodifiableSet(confDict.conflictsOf(this));
    }

    /**
     * Returns {@code true} iff this enum constant coflicts with {@code other};
     *         a modifier does not conflict with itself.
     *
     * @param other a modifier
     * @return {@code true} if this enum constant coflicts with {@code other};
     *         {@code false} otherwise
     */
    public boolean conflictsWith(Modifier other) {
        return conflictSet().contains(other);
    }

    /**
     * Returns {@code true} iff this enum constant coflicts with {@code other}.
     *
     * @param other a collection of modifiers
     * @return {@code true} if this enum constant coflicts with {@code other};
     *         {@code false} otherwise
     */
    public boolean conflictsWith(Collection<? extends Modifier> other)
    {
        for (Modifier m : other)
            if (conflictSet().contains(m))
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the specified collection contains conflicting 
     * modifiers.
     *
     * @param other a collection of modifiers
     * @return {@code true} if the specified collection contains conflicting 
     *         modifiers
     */
    public static boolean hasConflict(Collection<? extends Modifier> other)
    {
        for (Modifier m : other)
            if (m.conflictsWith(other))
                return true;
        return false;
    }

    /**
     * Returns {@code true} iff this modifier applies to the specified element.
     *
     * @param type an element type
     * @return {@code true} if this modifier applies to the specified element; 
     *         {@code false} otherwise
     * @see java.lang.annotation.ElementType
     */
    public boolean appliesTo(ElementType type) {
        return types.contains(type);
    }

    /**
     * Returns the corresponding ASM Constant.
     *
     * @return the corresponding ASM Constant Value
     * @see org.apache.bcel.Constants
     */
    public int code() { return modifier; }

    /**
     * Returns the modifier that corresponds to the 
     * specified ASM Constant.
     *
     * @param modifier the ASM Constant Value of the modifier to be returned
     * @return the enum constant with the specified ASM Constant Value
     * @see org.objectweb.asm.Opcodes
     */
    public static Modifier valueOf(int modifier) {
        for (Modifier m : values())
            if (m.code() == modifier)
                return m;
        throw new IllegalArgumentException("Unknown Modifier: " + modifier);
    }

    /**
     * Returns a modifier by parsing a string that serves as its exact 
     * textual represenation.
     *
     * @param symbol the textual representation of a modifier
     * @return the parsed enum constant
     */
    public static Modifier fromString(String symbol)
    {
        Modifier m = stringToEnum.get(symbol);

        if (m == null)
            throw new IllegalArgumentException("Unknown Modifier: " + symbol);
        
        return m;
    }

    /**
     * Returns the exact textual representation of this modifier.
     * 
     * @return the exact textual representation of this modifier
     */
    public String toString() {
        return repr == null ? super.toString().toLowerCase() : repr;
    }

    /**
     * Transforms an ASM bit field to a modifier set.
     *
     * @param modifiers an ASM bit field
     * @return the equivalent modifier set for {@code modifiers}
     * @throws IllegalArgumentException if {@code modifiers}
     *         contain unknown constants
     * @see org.objectweb.asm.Opcodes
     */
    public static EnumSet<Modifier> decode(int modifiers)
        throws IllegalModifierException
    {
        EnumSet<Modifier> am = EnumSet.noneOf(Modifier.class);
        
        for (Modifier mod : Modifier.values())
            if ((mod.code() & modifiers) != 0)
                am.add(mod);

        /* Check for unknown code */
        int mask = modifiers & ~encode(am);

        if (mask != 0)
            throw new IllegalModifierException(mask);
        return am;
    }

    /**
     * Transforms a modifier collection to a ASM bit field.
     *
     * @param modifiers a modifier collection
     * @return the ASM equivalent bit field for {@code modifiers}
     * @see org.objectweb.asm.Opcodes
     */
    public static int encode(Collection<? extends Modifier> modifiers)
    {
        int code = 0;

        for (Modifier m : modifiers)
            code |= m.code();

        return code;
    }

    /**
     * Performs a sanity check for a set of modifiers; checks to see that
     * no overlapping access control modifiers exist.
     *
     * @param modifiers a modifier set
     * @return {@code true} if the modifier set contains more than one
     *         access control modifiers ({@code private, public, protected}); 
     *         {@code false} otherwise
     */
    public static boolean atMostOne(Set<? extends Modifier> modifiers)
    {
        int total = 0;

        if (modifiers.contains(PRIVATE))
            total++;

        if (modifiers.contains(PUBLIC))
            total++;

        if (modifiers.contains(PROTECTED))
            total++;

        return total <= 1;
    }

    /**
     * Performs testing and prints various information about the 
     * modifiers supplied by {@code args}.
     *
     * @param args an array of modifiers to which the testing will apply;
     *        if array is empty, then some general tests are run and a list
     *        of available modifiers is printed
     *
     * @throws IllegalArgumentException if {@code args} contains an unknown
     *         modifier
     */
    public static void main(String [] args) throws IllegalModifierException
    {
        if (args.length == 0) {
            System.out.println("Printing all modifiers: ");
            for (Modifier m : values())
                System.out.println("    " + m);

            System.out.println("\nTesting encoding/decoding to ASM constants...");

            int mask = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT;
                
            assert mask == encode(EnumSet.of(PUBLIC, FINAL, TRANSIENT));

            Set<Modifier> modifiers = decode(mask);

            assert modifiers.contains(PUBLIC);
            assert modifiers.contains(FINAL);
            assert modifiers.contains(TRANSIENT);
            assert modifiers.size() == 3;

            System.out.println("OK");
            return;
        }
        
        Set<Modifier> modifiers = new HashSet<Modifier>();
        
        for (String arg : args)
        {
            Modifier m = Modifier.fromString(arg);
            modifiers.add(m);

            System.out.println("Modifier " + m);

            System.out.println(
                "    " +
                (m.appliesTo(TYPE) ?
                 "applies to classes" :
                 "does not apply to classes"));
            
            System.out.println(
                "    " +
                (m.appliesTo(FIELD) ?
                 "applies to fields" :
                 "does not apply to fields"));

            System.out.println(
                "    " +
                (m.appliesTo(METHOD) ?
                 "applies to methods" :
                 "does not apply to methods"));

            System.out.println(
                "    " +
                (m.appliesTo(CONSTRUCTOR) ?
                 "applies to constructors" :
                 "does not apply to constructors"));

            for (Modifier conf : m.conflictSet())
                System.out.println("    conflicts with " + conf);
            
            System.out.println("    ASM constant: " + m.code());
            System.out.println();
        }

        assert modifiers.equals(decode(encode(modifiers)));
        System.out.println("ASM mask: " + encode(modifiers));
    }

    public static class IllegalModifierException extends Exception
    {
        protected static final long serialVersionUID = 2376872342634L;

        public IllegalModifierException(int flag) {
            super("Flags contain some unknown modifier(s): " + flag);
        }
    }
}
