package org.clyze.jphantom.access;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.clyze.jphantom.constraints.*;

public abstract class AccessStateMachine 
    extends StateMachine<State,Event>
    implements Opcodes
{
    private final List<Constraint> constraints = new LinkedList<>();

    protected AccessStateMachine(State initial) {
        super(initial);
    }

    protected void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    public State get(State from, Event event)
    {
        State to = super.get(from, event);

        if (to == null)
            throw new IllegalTransitionException(from, event);

        return to;
    }

    public class EventSequence {
        private State current = initial;

        public EventSequence moveTo(Event event) {
            current = get(current, event);
            return this;
        }

        public int getCurrentAccess() {
            return current.getAccess();
        }
    }
}
