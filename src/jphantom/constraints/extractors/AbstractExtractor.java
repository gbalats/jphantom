package jphantom.constraints.extractors;

import jphantom.*;
import jphantom.exc.*;
import jphantom.tree.*;
import jphantom.tree.closure.*;
import jphantom.conversions.*;
import jphantom.constraints.solvers.*;

import org.objectweb.asm.*;

public abstract class AbstractExtractor implements ConversionVisitor, StandardTypes
{
    protected final TypeConstraintSolver solver;
    protected final ClassHierarchy hierarchy;
    private final ClassHierarchy.Snapshot closure;

    public AbstractExtractor(TypeConstraintSolver solver)
    {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        this.closure = new CopyingSnapshot(hierarchy);
    }
    
    /////////////////// Conversion Visitor ///////////////////

    @Override
    public void visit(IdentityConversion conv) {}

    @Override
    public void visit(WideningPrimitiveConversion conv) {}

    @Override
    public void visit(NarrowingPrimitiveConversion conv) {}

    @Override
    public void visit(IllegalConversion conv) {
        throw new InsolvableConstraintException(conv.asConstraint());
    }

    @Override
    public void visit(NullConversion conv) {}

    @Override
    public void visit(WideningReferenceConversion conv)
    {
        // Object is a supertype for every reference type
        if (conv.to.equals(OBJECT))
            return;

        // Array types => add contraint for elements
        if (conv.from.getSort() == Type.ARRAY && 
            conv.to.getSort() == Type.ARRAY)
        {
            Type from = ArrayType.elementOf(conv.from);
            Type to = ArrayType.elementOf(conv.to);

            // Conversion for element types
            Conversion subconv = Conversions.getAssignmentConversion(from, to);

            subconv.accept(this);
        }

        // Skip array reference types
        if (conv.from.getSort() == Type.ARRAY || 
            conv.to.getSort() == Type.ARRAY)
            return;
                
        // Non-array reference types

        if (hierarchy.contains(conv.from) && hierarchy.contains(conv.to)) {
            try {
                // Check if conversion is legal w.r.t. our class hierarchy
                if (!closure.isSubtypeOf(conv.from, conv.to))
                    throw new InsolvableConstraintException(conv.asConstraint());
                return;
            } catch (IncompleteSupertypesException exc) {} // Add constraint below
        }

        // Add constraint to underlying solver
        conv.asConstraint().accept(solver);
        return;
    }
}
