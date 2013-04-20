package jphantom.methods;

import java.util.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.commons.Method;
// import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MethodSignature extends Method implements Opcodes
{
    private String repr;
    private final int access;
    private final List<Type> exceptions;

    private MethodSignature(Builder builder)
    {
        super(builder.name, builder.desc);
        this.access = builder.access;
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

    public int getAccess() {
        return access;
    }

    public String toString() {
        // Lazy initialization
        if (repr != null) { return repr; }

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

        return repr = builder.toString();
    }

    private String toString(Type t) {
        return t.getClassName().replaceFirst("java\\.lang\\.", "");
    }

    // public boolean equals(Object obj) {
    //     if (this == obj)
    //         return true;
    //     if (!(obj instanceof MethodSignature))
    //         return false;
    //     MethodSignature other = (MethodSignature) obj;

    //     return super.equals(other) && 
    //         exceptions.equals(other.exceptions) && 
    //         modifiers.equals(other.modifiers);
    // }

    // public int hashCode() {
    //     return new HashCodeBuilder(17,37)
    //         .append(exceptions)
    //         .append(modifiers)
    //         .appendSuper(super.hashCode())
    //         .toHashCode();
    // }

    public boolean isAbstract() {
        return (access & ACC_ABSTRACT) != 0;
    }

    public boolean isPrivate() {
        return (access & ACC_PRIVATE) != 0;
    }

    public static MethodSignature fromMethodNode(MethodNode node)
    {
        return new Builder(node.name, node.desc)
            .access(node.access)
            .exceptions(node.exceptions.toArray(new String[0]))
            .build();
    }

    private void appendAccess(StringBuilder buf)
    {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            buf.append("public ");
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            buf.append("private ");
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            buf.append("protected ");
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            buf.append("final ");
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            buf.append("static ");
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            buf.append("synchronized ");
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            buf.append("volatile ");
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            buf.append("transient ");
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            buf.append("abstract ");
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            buf.append("strictfp ");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            buf.append("synthetic ");
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            buf.append("enum ");
        }
    }
}
