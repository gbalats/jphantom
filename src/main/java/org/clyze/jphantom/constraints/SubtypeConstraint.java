package org.clyze.jphantom.constraints;

import com.esotericsoftware.reflectasm.FieldAccess;
import org.jgrapht.graph.DefaultEdge;
import org.objectweb.asm.Type;
import org.jgrapht.EdgeFactory;

public class SubtypeConstraint extends DefaultEdge implements Constraint
{
    private static final FieldAccess parentAccessor;
    public final Type subtype;
    public final Type supertype;

    public SubtypeConstraint(Type subtype, Type supertype)
    {
        if (subtype == null)
            throw new IllegalArgumentException();
        if (supertype == null)
            throw new IllegalArgumentException();

        this.subtype = subtype;
        this.supertype = supertype;

        try {
            // Set "IntrusiveEdge" values, which JGraphT internally uses to optimize fetching vertices of an edge
            parentAccessor.set(this, "source", subtype);
            parentAccessor.set(this, "target", supertype);
        } catch (Throwable ignored) {
            // Newer JDK's can throw an access exception if you don't open the reflection access.
        }
    }

    @Override
    public void accept(ConstraintVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return subtype.getClassName() + " <: " + supertype.getClassName();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (!(other instanceof SubtypeConstraint))
            return false;

        SubtypeConstraint o = (SubtypeConstraint) other;

        return subtype.equals(o.subtype) && supertype.equals(o.supertype);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + subtype.hashCode();
        result = 31 * result + supertype.hashCode();
        return result;
    }

    public static final Factory factory = new Factory();

    public static class Factory implements EdgeFactory<Type,SubtypeConstraint>
    {
        private Factory() {}

        @Override
        public SubtypeConstraint createEdge(Type source, Type target)
        {
            return new SubtypeConstraint(source, target);
        }
    }


    static {
        try {
            parentAccessor = FieldAccess.get(Class.forName("org.jgrapht.graph.IntrusiveEdge"));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException();
        }
    }
}
