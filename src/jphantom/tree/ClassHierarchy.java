package jphantom.tree;

import java.util.*;
import jphantom.Types;
import org.objectweb.asm.Type;

/** @author George Balatsouras */
public interface ClassHierarchy extends Iterable<Type>, Types
{    
    void addClass(Type clazz, Type superclass, Type[] interfaces);
    void addInterface(Type iface, Type[] superInterfaces);

    /**
     * Returns true if this object type is an interface.
     * 
     * @param obj an object type
     * @return true if this object type is an interface
     * @throws IllegalArgumentException if the argument is not an object type
     * @throws TypeNotPresentException if the class hierarchy does not contain 
     *         this object type
     */
    boolean isInterface(Type obj);

    /**
     * Returns true if the object type is contained in this class hierarchy.
     * 
     * @param obj an object type
     * @return true if the object type is contained in this class hierarchy
     * @throws IllegalArgumentException if the argument is not an object type
     */
    boolean contains(Type obj);

    /**
     * Returns the direct interface supertypes of this object type.
     * 
     * @param obj an object type
     * @return direct interface supertypes
     * @throws IllegalArgumentException if the argument is not an object type
     * @throws TypeNotPresentException if the class hierarchy does not contain 
     *         this object type
     */
    Set<Type> getInterfaces(Type obj);

    /**
     * Returns the direct superclass of this object type.
     * ({@code java.lang.Object} in case of an interface type).
     * 
     * @param obj an object type
     * @return direct superclass of this object type
     * @throws IllegalArgumentException if the argument is not an object type
     * @throws TypeNotPresentException if the class hierarchy does not contain 
     *         this object type
     */
    Type getSuperclass(Type obj);


    /////////////// Transitive Closure ///////////////

    interface Snapshot extends ClassHierarchy {

        /**
         * Returns a list of all class supertypes. The list always starts with
         * the direct superclass and ends with {@code java.lang.Object}. It will
         * be empty in the case of {@code java.lang.Object}.
         * 
         * @param obj an object type
         * @return a set of all the transitively computed superclasses in case 
         *         of a class type
         *         or {@code (java.lang.Object)} in case of an interface type
         * @throws IncompleteSupertypesException if a supertype has not been added yet
         * @throws IllegalArgumentException if the argument is not an object type
         * @throws TypeNotPresentException if the class hierarchy does not contain 
         *         this object type
         */
        List<Type> getAllSuperclasses(Type obj) throws IncompleteSupertypesException;

        /**
         * Returns the set of all interface supertypes.
         * 
         * @param obj an object type
         * @return a set of all the transitively implemented interfaces of a class type
         *         or the set of all super-interfaces of an interface type
         * @throws IllegalArgumentException if the argument is not an object type
         * @throws TypeNotPresentException if the class hierarchy does not contain 
         *         this object type
         */
        Set<Type> getAllInterfaces(Type obj) throws IncompleteSupertypesException;


        Set<Type> getAllSupertypes(Type obj) throws IncompleteSupertypesException;

        boolean isSubtypeOf(Type type, Type supertype) 
            throws IncompleteSupertypesException;

        boolean isStrictSubtypeOf(Type type, Type supertype) 
            throws IncompleteSupertypesException;

        Type firstCommonSuperclass(Type a, Type b)
            throws IncompleteSupertypesException;
        
    }
}
