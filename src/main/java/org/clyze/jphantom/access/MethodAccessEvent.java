package org.clyze.jphantom.access;

public class MethodAccessEvent extends Event
{
    public final String name;
    public final String desc;

    private MethodAccessEvent(Builder builder) {
        super(builder.opcode);
        name = builder.mName;
        desc = builder.desc;
    }

    @Override
    public String toString() {
        return "Accessing method: " + name + desc + 
            " " + super.toString();
    }

    public static class Builder
    {
        private String mName = null;
        private String desc = null;
        private int opcode = NOP;

        public Builder() {}

        public MethodAccessEvent build() {
            return new MethodAccessEvent(this);
        }

        public Builder setName(String name) {
            this.mName = name;
            return this;
        }

        public Builder setDescriptor(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder setOpcode(int opcode) {
            switch (opcode) {
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE: break;
            default:
                throw new IllegalArgumentException(
                    "Illegal Method Access Instruction Opcode: " + opcode);
            }
            this.opcode = opcode;
            return this;
        }
    }
}
