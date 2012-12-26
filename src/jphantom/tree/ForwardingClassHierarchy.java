package jphantom.tree;

import java.util.*;
import org.objectweb.asm.Type;

public class ForwardingClassHierarchy extends AbstractClassHierarchy
{
    protected final ClassHierarchy hierarchy;

    public ForwardingClassHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces) {
        hierarchy.addClass(clazz, superclass, interfaces);
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces) {
        hierarchy.addInterface(iface, superInterfaces);
    }

    @Override
    public Iterator<Type> iterator() {
        return hierarchy.iterator();
    }

    @Override
    public boolean isInterface(Type obj) {
        return hierarchy.isInterface(obj);
    }

    @Override
    public boolean contains(Type obj) {
        return hierarchy.contains(obj);
    }

    @Override
    public Set<Type> getInterfaces(Type obj) {
        return hierarchy.getInterfaces(obj);
    }

    @Override
    public Type getSuperclass(Type obj) {
        return hierarchy.getSuperclass(obj);
    }
}
