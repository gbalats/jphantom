package org.clyze.jphantom.constraints.solvers;

import java.util.*;
import org.clyze.jphantom.exc.*;
import org.clyze.jphantom.hier.*;
import org.clyze.jphantom.constraints.*;
import org.objectweb.asm.Type;

public interface TypeConstraintSolver 
    extends Solver<Type,SubtypeConstraint,ClassHierarchy>, ConstraintVisitor
{
    TypeConstraintSolver solve() throws UnsatisfiableStateException;
    void markClass(Type vertex);
    void markInterface(Type vertex);
    Collection<Constraint> getConstraints();
    ClassHierarchy getHierarchy();
    void setHierarchy(ClassHierarchy hierarchy);
}
