package jphantom.fields;

import jphantom.Signature;
import java.util.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

public class FieldSignature extends Signature
{
    private final String name;
    private final String desc;

    private FieldSignature(Builder builder)
    {
        super(builder.access);
        this.name = builder.name;
        this.desc = builder.desc;
    }

    public static class Builder {
        private final String name;
        private final String desc;

        private int access = 0;

        public Builder(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public Builder access(int access) {
            this.access = access;
            return this;
        }

        public FieldSignature build() {
            return new FieldSignature(this);
        }
    }

    public Type getType() {
        return Type.getType(desc);
    }

    public String getDescriptor() {
        return desc;
    }

    @Override
    public String toStringAux() {
        StringBuilder builder = new StringBuilder();

        // Access Modifiers
        appendAccess(builder);

        // Named Descriptor
        builder.append(toString(getType()) + " " + name + "(");

        return builder.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FieldSignature))
            return false;

        FieldSignature other = (FieldSignature) obj;

        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static FieldSignature fromFieldNode(FieldNode node)
    {
        return new Builder(node.name, node.desc)
            .access(node.access)
            .build();
    }
}
