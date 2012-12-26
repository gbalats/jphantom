package jphantom.tree;

import util.*;
import java.util.*;
import jphantom.*;
import org.objectweb.asm.Type;
import static util.Utils.*;

/** @author George Balatsouras */
public class IncrementalClassHierarchy extends AbstractClassHierarchy 
    implements StandardTypes
{    
    ///////////////// Fields /////////////////

    private final Map<Type,Type> directSuperclass = newMap();
    private final Map<Type,Set<Type>> directImplInterfaces = newMap();
    private final Map<Type,Boolean> isIface = newMap();


    ///////////////// Constructors /////////////////

    public IncrementalClassHierarchy()
    {
        super();

        // Add java.lang.Object

        directSuperclass.put(OBJECT, null);
        directImplInterfaces.put(OBJECT, Collections.<Type>emptySet());
        isIface.put(OBJECT, false);
    }

    public IncrementalClassHierarchy(ClassHierarchy other)
    {
        this();

        for (Type t : other) {
            isIface.put(t, other.isInterface(t));

            Set<Type> ifaces = Collections.unmodifiableSet(
                other.getInterfaces(t));

            directSuperclass.put(t, other.getSuperclass(t));
            directImplInterfaces.put(t, ifaces);
        }
    }

    ///////////////////////// Methods /////////////////////////

    @Override
    public Iterator<Type> iterator() {
        return isIface.keySet().iterator();
    }

    private final void addSupertypes(Type clazz, Type superclass, Type[] interfaces)
    {
        // Create an unmodifiable set of interfaces
        Set<Type> ifaces = Collections.unmodifiableSet(
            newSet(Arrays.asList(interfaces)));

        assert !ifaces.contains(null);
        assert superclass != null || clazz.equals(OBJECT) : clazz;

        directImplInterfaces.put(clazz, ifaces);
        directSuperclass.put(clazz, superclass);
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces)
    {
        if (contains(clazz))
            throw new IllegalArgumentException(clazz + " has already been added");

        // Argument Checking

        for (Type i : interfaces)
            checkedInterface(i);

        checkedClass(superclass);

        // No graph cycle sanity check is done at this point

        isIface.put(clazz, false);
        addSupertypes(clazz, superclass, interfaces);
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces) 
    {
        if (contains(iface))
            throw new IllegalArgumentException(iface + " has already been added");

        // Argument Checking

        for (Type i : superInterfaces)
            checkedInterface(i);
     
        // No graph cycle sanity check is done at this point

        isIface.put(iface, true);
        addSupertypes(iface, OBJECT, superInterfaces);
    }

    @Override
    public final boolean isInterface(Type obj) {
        return isIface.get(checkedContainedObject(obj));
    }

    @Override
    public final boolean contains(Type obj) {
        return isIface.containsKey(checkedObject(obj));
    }

    @Override
    public Set<Type> getInterfaces(Type obj)
    {
        Set<Type> ifaces = directImplInterfaces.get(checkedContainedObject(obj));

        // Sanity checks
        assert ifaces != null : obj;

        return ifaces;
    }

    @Override
    public Type getSuperclass(Type obj)
    {
        Type superclass = directSuperclass.get(checkedContainedObject(obj));

        // Sanity checks
        assert superclass != null || obj.equals(OBJECT) : obj;
        assert !isInterface(obj) || superclass.equals(OBJECT);

        return superclass;
    }
}

