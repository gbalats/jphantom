package jphantom.constraints.solvers;

import java.util.*;
import jphantom.exc.*;
import jphantom.hier.*;
import jphantom.constraints.*;
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
