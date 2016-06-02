package org.clyze.jphantom.constraints;

public interface Constraint {
    void accept(ConstraintVisitor visitor);
}
