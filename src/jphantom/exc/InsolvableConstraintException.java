package jphantom.exc;

import jphantom.constraints.Constraint;

public class InsolvableConstraintException extends RuntimeException
{
    protected static final long serialVersionUID = 386345834576357L;

    public InsolvableConstraintException(Constraint constraint) {
        super(constraint.toString());
    }
}
