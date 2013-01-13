package jphantom.access;

import org.objectweb.asm.Opcodes;

public class Event implements Opcodes
{
    private final int opcode;

    protected Event(int opcode) {
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof Event))
            return false;
        Event other = (Event) obj;
        return opcode == other.opcode;
    }

    @Override
    public int hashCode() {
        return opcode;
    }

    @Override
    public String toString() {
        return "Opcode received: " + opcode;
    }
}
