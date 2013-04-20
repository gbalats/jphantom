package jphantom.constraints.solvers;

import java.util.*;
import jphantom.constraints.*;

public class ConstraintStoringSolver extends ForwardingSolver
{
    private final Set<Constraint> constraints = new HashSet<>();
    private final Set<Constraint> immutableConstraints = 
        Collections.unmodifiableSet(constraints);

    public ConstraintStoringSolver(TypeConstraintSolver solver) {
        super(solver);
    }

    public Collection<Constraint> getConstraints() {
        return immutableConstraints;
    }

    ///////////////// Store Constraints /////////////////

    @Override
    public void visit(IsanInterfaceConstraint constraint) {
        constraints.add(constraint);
        super.visit(constraint);
    }

    @Override
    public void visit(IsaClassConstraint constraint) {
        constraints.add(constraint);
        super.visit(constraint);
    }

    @Override
    public void visit(SubtypeConstraint constraint) {
        constraints.add(constraint);
        super.visit(constraint);
    }
}
