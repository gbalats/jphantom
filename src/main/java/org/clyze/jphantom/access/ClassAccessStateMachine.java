package org.clyze.jphantom.access;

import java.util.*;
import org.objectweb.asm.Type;
import org.clyze.jphantom.constraints.*;

import com.google.common.collect.*;

public class ClassAccessStateMachine extends AccessStateMachine
{
    @Override
    protected Table<State,Event,State> delegate() {
        return null;
    }

    /////////////////////// Singleton Pattern ///////////////////////

    protected ClassAccessStateMachine() {
        super(null);
    }

    public static final ClassAccessStateMachine instance = 
        new ClassAccessStateMachine();

    public static ClassAccessStateMachine v() { return instance; }


    /////////////////////// Sequence Inner Class ///////////////////////

    public class EventSequence extends AccessStateMachine.EventSequence
    {
        private final Type owner;
        private int access = -1;

        private EventSequence(Type owner) {
            this.owner = owner;
        }

        public EventSequence moveTo(ClassAccessEvent event)
        {
            // TODO: Process Access Flags
            // May include: 
            // public, protected, private, static, final, interface, abstract
            // The JVM currently does not check the consistency of the 
            // class that is reference and of the actual class file.

            // Notes: bytecode is entirely oblivious to nested/inner 
            // classes. An inner class has the exact same representation 
            // to an outer class except from the references to the 
            // enclosing object.

            // Source => Inner Class Attribute
            // 
            // interface, abstract => abstract
            // final => final
            // public, protected => public
            // static => (empty)
            // (empty), private => (empty)

            if ((event.access & ACC_INTERFACE) != 0)
                addConstraint(new IsaClassConstraint(owner));

            this.access = event.access;
            return this;
        }

        @Override
        public int getCurrentAccess() {
            return access;
        }
    }

    private Map<Type,EventSequence> sequences = new HashMap<>();
    
    public EventSequence getEventSequence(Type owner)
    {
        if (!sequences.containsKey(owner))
            sequences.put(owner, new EventSequence(owner));

        return sequences.get(owner);
    }
}
