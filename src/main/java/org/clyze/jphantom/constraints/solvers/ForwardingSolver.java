package org.clyze.jphantom.constraints.solvers;

import java.util.*;
import org.clyze.jphantom.exc.*;
import org.clyze.jphantom.hier.*;
import org.clyze.jphantom.constraints.*;
import org.jgrapht.*;
import org.objectweb.asm.Type;

public class ForwardingSolver implements TypeConstraintSolver 
{
    private final TypeConstraintSolver solver;

    public ForwardingSolver(TypeConstraintSolver solver) {
        this.solver = solver;
    }

    @Override
    public Collection<Constraint> getConstraints() {
        return solver.getConstraints();
    }

    @Override
    public ClassHierarchy getHierarchy() {
        return solver.getHierarchy();
    }

    @Override
    public void setHierarchy(ClassHierarchy hierarchy) {
        solver.setHierarchy(hierarchy);
    }

    @Override
    public TypeConstraintSolver solve() throws UnsatisfiableStateException
    {
        solver.solve();
        return this;
    }

    @Override
    public void visit(SubtypeConstraint constraint) {
        solver.visit(constraint);
    }

    @Override
    public void visit(IsanInterfaceConstraint constraint) {
        solver.visit(constraint);
    }

    @Override
    public void visit(IsaClassConstraint constraint) {
        solver.visit(constraint);
    }

    @Override
    public DirectedGraph<Type,SubtypeConstraint> getConstraintGraph() {
        return solver.getConstraintGraph();
    }

    @Override
    public ClassHierarchy getSolution() {
        return solver.getSolution();
    }

    @Override
    public void addConstraintEdge(Type source, Type target) {
        solver.addConstraintEdge(source, target);
    }

    @Override
    public void markClass(Type vertex) {
        solver.markClass(vertex);
    }

    @Override
    public void markInterface(Type vertex) {
        solver.markInterface(vertex);
    }

}
