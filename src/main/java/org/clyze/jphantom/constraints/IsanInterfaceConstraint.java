package org.clyze.jphantom.constraints;

import org.objectweb.asm.Type;

public class IsanInterfaceConstraint extends IsaConstraint
{
    public IsanInterfaceConstraint(Type type) {
        super(type);
    }

    @Override
    public void accept(ConstraintVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return type.getClassName() + " must be an interface";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IsanInterfaceConstraint))
            return false;
        IsanInterfaceConstraint other = (IsanInterfaceConstraint) o;
        return type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
