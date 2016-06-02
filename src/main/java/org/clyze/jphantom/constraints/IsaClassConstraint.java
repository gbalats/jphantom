package org.clyze.jphantom.constraints;

import org.objectweb.asm.Type;

public class IsaClassConstraint extends IsaConstraint
{
    public IsaClassConstraint(Type type) {
        super(type);
    }

    @Override
    public void accept(ConstraintVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return type.getClassName() + " must be a class";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IsaClassConstraint))
            return false;
        IsaClassConstraint other = (IsaClassConstraint) o;
        return type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
