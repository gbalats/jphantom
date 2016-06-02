package org.clyze.jphantom.constraints;

public interface ConstraintVisitor
{
    void visit(SubtypeConstraint constraint);
    void visit(IsaClassConstraint constraint);
    void visit(IsanInterfaceConstraint constraint);
}
