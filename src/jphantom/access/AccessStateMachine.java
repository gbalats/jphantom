package jphantom.access;

import java.util.*;
import jphantom.constraints.*;
import static util.Utils.*;

public abstract class AccessStateMachine<C extends AccessContext>
{
    private static final List<Constraint> constraints = newList();

    protected Set<Modifier> state = EnumSet.noneOf(Modifier.class);

    public abstract AccessStateMachine<C> moveTo(C ctx);

    public int getCurrentAccess() {
        return Modifier.encode(state);
    }

    protected static void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public static Collection<Constraint> getConstraints() {
        return constraints;
    }
}
