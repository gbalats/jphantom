package jphantom.hier;

import java.util.*;
import org.objectweb.asm.Type;

public class UnmodifiableClassHierarchy extends ForwardingClassHierarchy
{
    public UnmodifiableClassHierarchy(ClassHierarchy hierarchy) {
        super(hierarchy);
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces) {
        throw new UnsupportedOperationException();
    }
}
