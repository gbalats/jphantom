package org.clyze.jphantom.access;

import java.util.*;
import org.objectweb.asm.Type;
import org.clyze.jphantom.constraints.*;

import com.google.common.collect.*;

public class MethodAccessStateMachine extends AccessStateMachine
{
    /////////////////////// States ///////////////////////

    public static final State INIT_STATE = new State(ACC_PRIVATE).asPublic();
    public static final State VIRTUAL_STATE = new State().asPublic();
    public static final State STATIC_STATE = new State(ACC_STATIC).asPublic();
    public static final State INTERFACE_STATE = new State(ACC_ABSTRACT).asPublic();

    // Initial state should not be accepted.
    // That is why we encode an illegal access modifier (private + public)
    // to make sure that no such state is accepted as the final one.

    /////////////////////// Transition Table ///////////////////////

    private final Table<State,Event,State> transitions = 
        new ImmutableTable.Builder<State,Event,State>()
        .put(INIT_STATE, new Event(INVOKEVIRTUAL), VIRTUAL_STATE)
        .put(INIT_STATE, new Event(INVOKESPECIAL), VIRTUAL_STATE)
        .put(INIT_STATE, new Event(INVOKESTATIC), STATIC_STATE)
        .put(INIT_STATE, new Event(INVOKEINTERFACE), INTERFACE_STATE)
        .put(VIRTUAL_STATE, new Event(INVOKEVIRTUAL), VIRTUAL_STATE)
        .put(VIRTUAL_STATE, new Event(INVOKESPECIAL), VIRTUAL_STATE)
        .put(STATIC_STATE, new Event(INVOKESTATIC), STATIC_STATE)
        .put(INTERFACE_STATE, new Event(INVOKEINTERFACE), INTERFACE_STATE)
        .build();

    @Override
    protected Table<State,Event,State> delegate() {
        return transitions;
    }

    /////////////////////// Singleton Pattern ///////////////////////

    protected MethodAccessStateMachine() {
        super(INIT_STATE);
    }

    public static final MethodAccessStateMachine instance = 
        new MethodAccessStateMachine();

    public static MethodAccessStateMachine v() { return instance; }

    /////////////////////// Sequence Inner Class ///////////////////////

    public class EventSequence extends AccessStateMachine.EventSequence
    {
        private final String methodName;
        private final Type owner;
        private final String desc; // Lazy initialization

        private EventSequence(String name, Type owner, String desc) {
            this.methodName = name;
            this.owner = owner;
            this.desc = desc;
        }

        protected EventSequence checkDescriptor(String descriptor)
        {
            if (!desc.equals(descriptor))
                throw new IllegalStateException(
                    "Method \'" + owner.getClassName() + " " + methodName +
                    "\' has multiple descriptors: " + desc + " " + descriptor);
            return this;
        }

        public EventSequence moveTo(MethodAccessEvent event)
        {
            super.moveTo(event);

            switch (event.getOpcode()) {
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKESPECIAL:
                // <clinit> is never called explicitly
                // => name must be <init> => implies a class owner
                addConstraint(new IsaClassConstraint(owner));
                break;
            case INVOKEINTERFACE:
                addConstraint(new IsanInterfaceConstraint(owner));
                break;
            default:
                throw new AssertionError();
            }

            return checkDescriptor(event.desc);
        }
    }

    private Map<String,EventSequence> sequences = new HashMap<>();
    
    public EventSequence getEventSequence(String methodName, Type owner, String desc)
    {
        String key = owner.getClassName() + ":" + methodName + " " + desc;

        if (!sequences.containsKey(key))
            sequences.put(key, new EventSequence(methodName, owner, desc));

        return sequences.get(key);
    }
}
