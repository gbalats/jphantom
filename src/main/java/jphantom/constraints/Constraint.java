package jphantom.constraints;

public interface Constraint {
    void accept(ConstraintVisitor visitor);
}
