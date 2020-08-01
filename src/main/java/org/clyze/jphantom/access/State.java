package org.clyze.jphantom.access;

import org.objectweb.asm.Opcodes;

public class State implements Opcodes
{
    private final int access;

    protected State(int access) {
        this.access = access;
    }

    protected State() {
        this(0);
    }

    public int getAccess() {
        return access;
    }

    public State asPublic() {
        return (access & ACC_PUBLIC) == 0 ? 
            new State(access | ACC_PUBLIC) : this;
    }

    @Override
    public String toString() {
        return "Access Flags: " + String.format("%0#6X", access);
    }
}
