package jphantom.access;

import java.util.*;
import org.objectweb.asm.Type;
import jphantom.constraints.*;
import edu.umd.cs.findbugs.annotations.*;
import com.google.common.collect.*;

public class FieldAccessStateMachine extends AccessStateMachine
{
    /////////////////////// States ///////////////////////

    public static final State EMPTY_STATE = new State().asPublic();
    public static final State FINAL_STATE = new State(ACC_FINAL).asPublic();
    public static final State STATIC_STATE = new State(ACC_STATIC).asPublic();
    public static final State STATIC_FINAL_STATE = new State(ACC_STATIC | ACC_FINAL).asPublic();

    /////////////////////// Transition Table ///////////////////////

    private final Table<State,Event,State> transitions = 
        new ImmutableTable.Builder<State,Event,State>()
        .put(FINAL_STATE, new Event(GETFIELD), FINAL_STATE)
        .put(FINAL_STATE, new Event(PUTFIELD), EMPTY_STATE)
        .put(FINAL_STATE, new Event(GETSTATIC), STATIC_FINAL_STATE)
        .put(FINAL_STATE, new Event(PUTSTATIC), STATIC_STATE)
        .put(EMPTY_STATE, new Event(PUTFIELD), EMPTY_STATE)
        .put(EMPTY_STATE, new Event(GETFIELD), EMPTY_STATE)
        .put(STATIC_STATE, new Event(GETSTATIC), STATIC_STATE)
        .put(STATIC_STATE, new Event(PUTSTATIC), STATIC_STATE)
        .put(STATIC_FINAL_STATE, new Event(GETSTATIC), STATIC_FINAL_STATE)
        .put(STATIC_FINAL_STATE, new Event(PUTSTATIC), STATIC_STATE)
        .build();

    @Override
    protected Table<State,Event,State> delegate() {
        return transitions;
    }

    /////////////////////// Singleton Pattern ///////////////////////

    protected FieldAccessStateMachine() {
        super(FINAL_STATE);
    }

    public static final FieldAccessStateMachine instance = 
        new FieldAccessStateMachine();

    public static FieldAccessStateMachine v() { return instance; }


    /////////////////////// Sequence Inner Class ///////////////////////

    public class EventSequence extends AccessStateMachine.EventSequence
    {
        private final String fieldName;
        private final Type owner;
        private String desc; // Lazy initialization

        private EventSequence(String name, Type owner) {
            this.fieldName = name;
            this.owner = owner;
        }

        protected EventSequence checkDescriptor(String descriptor)
        {
            if (desc == null)
                desc = descriptor;
            else if (!desc.equals(descriptor))
                throw new IllegalStateException(
                    "Field \'" + owner.getClassName() + " " + fieldName +
                    "\' has multiple descriptors: " + desc + " " + descriptor);
            return this;
        }

        @SuppressFBWarnings(value = "SF_SWITCH_FALLTHROUGH")
        public EventSequence moveTo(FieldAccessEvent event)
        {
            super.moveTo(event);

            switch (event.getOpcode()) {
            case PUTSTATIC: case PUTFIELD: case GETFIELD: 
                addConstraint(new IsaClassConstraint(owner));
            case GETSTATIC:
                break;
            default:
                throw new AssertionError();
            }

            return checkDescriptor(event.desc);
        }
    }

    private Map<String,EventSequence> sequences = new HashMap<>();
    
    public EventSequence getEventSequence(String fieldName, Type owner)
    {
        String key = owner.getClassName() + ":" + fieldName;

        if (!sequences.containsKey(key))
            sequences.put(key, new EventSequence(fieldName, owner));

        return sequences.get(key);
    }
}
