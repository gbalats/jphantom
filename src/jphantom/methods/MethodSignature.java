package jphantom.methods;

import java.util.*;
import jphantom.Signature;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.commons.Method;

public class MethodSignature extends Signature
{
    private final Method m;
    private final List<Type> exceptions;

    private MethodSignature(Builder builder)
    {
        super(builder.access);
        this.m = new Method(builder.name, builder.desc);
        this.exceptions = Collections.unmodifiableList(
            Arrays.asList(builder.exceptions));
    }

    public static class Builder {
        private final String name;
        private final String desc;

        private int access = 0;
        private Type[] exceptions = new Type[0];

        public Builder(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public Builder access(int access) {
            this.access = access;
            return this;
        }

        public Builder exceptions(String ... exceptions)
        {
            if (exceptions != null) {
                this.exceptions = new Type[exceptions.length];

                int i = 0;

                for (String exc : exceptions)
                    this.exceptions[i++] = Type.getObjectType(exc);

            } else {
                this.exceptions = new Type[0];
            }
            return this;
        }

        public MethodSignature build() {
            return new MethodSignature(this);
        }
    }

    public List<Type> getExceptions() {
        return exceptions;
    }
    
    public String[] getExceptionNames()
    {
        String[] exceptionNames = new String[exceptions.size()];

        int i = 0;

        for (Type exception : exceptions)
            exceptionNames[i++] = exception.getInternalName();

        return exceptionNames;
    }

    @Override
    public String toStringAux()
    {    
        StringBuilder builder = new StringBuilder();

        // Access Modifiers
        appendAccess(builder);

        // Named Descriptor
        builder.append(toString(getReturnType()) + " " + getName() + "(");

        for (Type arg : getArgumentTypes())
            builder.append(toString(arg)).append(", ");
        
        if (getArgumentTypes().length > 0)
            builder.setLength(builder.length() - 2);
        
        builder.append(")");
        
        if (!exceptions.isEmpty()) {
            builder.append(" throws ");

            // Exceptions
            for (Type exc : exceptions)
                builder.append(toString(exc)).append(", ");

            builder.setLength(builder.length() - 2);
        }
        return builder.toString();
    }

    /* Delegated methods */

    @Override
    public boolean equals(Object obj) {
        return m.equals(obj);
    }

    @Override
    public int hashCode() {
        return m.hashCode();
    }

    public Type[] getArgumentTypes() {
        return m.getArgumentTypes();
    }

    public String getDescriptor() {
        return m.getDescriptor();
    }

    public String getName()  {
        return m.getName();
    }
    
    public Type getReturnType() {
        return m.getReturnType();
    }

    public static MethodSignature fromMethodNode(MethodNode node)
    {
        return new Builder(node.name, node.desc)
            .access(node.access)
            .exceptions(node.exceptions.toArray(new String[0]))
            .build();
    }
}
