package org.clyze.jphantom.hier.closure;

import java.util.*;
import org.clyze.jphantom.hier.*;
import org.objectweb.asm.Type;

public class PseudoSnapshot extends AbstractSnapshot
{    
    public PseudoSnapshot(ClassHierarchy hierarchy) {
        super(hierarchy);
    }

    //////////////// Transitive Computations ////////////////

    // Computes a list of all the superclasses of an object type.
    // Returns an empty list for Object, and a list containing just
    // Object for an interface. The result is a new list every time,
    // that does not contain the argument itself.
    
    @Override
    public List<Type> getAllSuperclasses(Type obj) throws IncompleteSupertypesException
    {
        checkedContainedObject(obj);
        List<Type> superclasses = new ArrayList<>();

        while((obj = getSuperclass(obj)) != null) {
            superclasses.add(obj);

            if (!contains(obj))
                throw new IncompleteSupertypesException(superclasses);
        }

        return superclasses;
    }

    // Computes a set of all the implemented interfaces of a class type,
    // or all the superinterfaces of an interface type. The result is a 
    // new list every time, that does not contain the argument itself.
    @Override
    public Set<Type> getAllInterfaces(Type obj) throws IncompleteSupertypesException
    {
        checkedContainedObject(obj);
        List<Type> superclasses;

        try {
            superclasses = getAllSuperclasses(obj);
        } catch (IncompleteSupertypesException exc) {
            superclasses = exc.getSupertypes();
        }

        try {
            Set<Type> supertypes = getAllSupertypes(obj);
            supertypes.removeAll(superclasses);
            return supertypes;
        } catch (IncompleteSupertypesException exc) {
            exc.getSupertypes().removeAll(superclasses);
            throw exc;
        }
    }

    @Override
    public Set<Type> getAllSupertypes(Type obj) throws IncompleteSupertypesException
    {
        checkedContainedObject(obj);
        Set<Type> supertypes = new HashSet<>();
        Queue<Type> queue = new LinkedList<Type>();
        boolean incomplete = false;
        
        do {
            queue.add(obj);

            while (!queue.isEmpty()) {
                Type t = queue.poll();

                supertypes.add(t);

                if (!contains(t)) {
                    incomplete = true;
                    continue;
                }

                for (Type i : getInterfaces(t))
                    queue.add(i);
            }
            if (!contains(obj))
                break;

        } while((obj = getSuperclass(obj)) != null);


        if (incomplete)
            throw new IncompleteSupertypesException(supertypes);

        return supertypes;
    }

    @Override
    public Type firstCommonSuperclass(Type a, Type b) throws IncompleteSupertypesException
    {
        checkedContainedObject(a);
        checkedContainedObject(b);
        Set<Type> visited = new HashSet<>();
        boolean incomplete = false;

        while (a != null || b != null) {
            if (a != null) {
                if (!visited.add(a))
                    return a;
                if (!contains(a))
                    incomplete = true;
                a = contains(a) ? getSuperclass(a) : null;
            }
            if (b != null) {
                if (!visited.add(b))
                    return b;
                if (!contains(b))
                    incomplete = true;
                b = contains(b) ? getSuperclass(b) : null;
            }
        }

        if (incomplete)
            throw new IncompleteSupertypesException();

        return null;
    }
}
