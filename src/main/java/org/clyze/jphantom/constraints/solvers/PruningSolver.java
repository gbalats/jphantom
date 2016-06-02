package org.clyze.jphantom.constraints.solvers;

import java.util.*;
import org.clyze.jphantom.hier.*;
import org.clyze.jphantom.constraints.*;
import org.objectweb.asm.Type;

public class PruningSolver extends ForwardingSolver
{
    private Set<Type> interesting = new HashSet<>();

    public PruningSolver(TypeConstraintSolver solver) {
        super(solver);
    }

    @Override
    public TypeConstraintSolver solve() throws UnsatisfiableStateException {
        setHierarchy(prunedHierarchy());
        return super.solve();
    }

    private ClassHierarchy prunedHierarchy()
    {
        ClassHierarchy hierarchy = getHierarchy();
        ClassHierarchy pruned = new IncrementalClassHierarchy();

        Importer importer = new Importer(pruned, hierarchy);

        for (Type t : interesting)
            if (hierarchy.contains(t))
                importer.execute(t);

        return pruned;
    }

    @Override
    public void visit(IsanInterfaceConstraint constraint) {
        interesting.add(constraint.type);
        super.visit(constraint);
    }

    @Override
    public void visit(IsaClassConstraint constraint) {
        interesting.add(constraint.type);
        super.visit(constraint);
    }

    @Override
    public void visit(SubtypeConstraint constraint) {
        interesting.add(constraint.subtype);
        interesting.add(constraint.supertype);
        super.visit(constraint);
    }

    @Override
    public void markClass(Type vertex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markInterface(Type vertex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConstraintEdge(Type source, Type target) {
        throw new UnsupportedOperationException();
    }
}
