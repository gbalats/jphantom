package jphantom.tree;

import org.objectweb.asm.Type;

public abstract class AbstractClassHierarchy implements ClassHierarchy
{
    public AbstractClassHierarchy() {}

    //////////////// Auxiliary Methods ////////////////

    // Simple check than ensures that the argument is an object type
    protected final static Type checkedObject(Type obj)
    {
        if (obj.getSort() != Type.OBJECT)
            throw new IllegalArgumentException(obj + " is not an object type");

        return obj;
    }

    // Simple check than ensures that the argument is an object type
    // and is contained in this class hierarchy
    protected final Type checkedContainedObject(Type obj)
    {
        if (!contains(checkedObject(obj)))
            throw new TypeNotPresentException(obj.getClassName(), null);
        
        return obj;
    }

    // Simple check than ensures that the argument is a class type
    // and is contained in this class hierarchy
    protected final Type checkedContainedClass(Type obj)
    {
        if (isInterface(obj))
            throw new IllegalArgumentException(obj + " is not a class");
        
        return obj;
    }

    // Simple check than ensures that the argument is an interface type
    // and is contained in this class hierarchy
    protected final Type checkedContainedInterface(Type obj)
    {
        if (!isInterface(obj))
            throw new IllegalArgumentException(obj + " is not an interface");
        
        return obj;
    }

    // Simple check than ensures that the argument is a class type
    protected final Type checkedClass(Type obj) {
        return contains(obj) ? checkedContainedClass(obj) : checkedObject(obj);
    }

    // Simple check than ensures that the argument is an interface type
    protected final Type checkedInterface(Type obj) {
        return contains(obj) ? checkedContainedInterface(obj) : checkedObject(obj);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Type t : this) {
            boolean iface = isInterface(t);
            
            builder
                .append(iface ? "interface " : "class ")
                .append(t.getClassName())
                .append('\n');

            Type sc = getSuperclass(t);

            if (sc != null && !iface)
                builder
                    .append("   extends ")
                    .append(sc.getClassName())
                    .append('\n');

            for (Type i : getInterfaces(t))
                builder
                    .append("   ")
                    .append(iface ? "extends " : "implements ")
                    .append(i.getClassName())
                    .append('\n');
        }
        return builder.toString();
    }
}
