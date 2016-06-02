package org.clyze.jphantom.access;

public class FieldAccessEvent extends Event
{
    public final String name;
    public final String desc;

    private FieldAccessEvent(Builder builder) {
        super(builder.opcode);
        name = builder.fName;
        desc = builder.desc;
    }

    public String toString() {
        return "Accessing field: " + name + desc + 
            " " + super.toString();
    }

    public static class Builder
    {
        private String fName = null;
        private String desc = null;
        private int opcode = NOP;

        public Builder() {}

        public FieldAccessEvent build() {
            return new FieldAccessEvent(this);
        }

        public Builder setName(String name) {
            this.fName = name;
            return this;
        }

        public Builder setDescriptor(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder setOpcode(int opcode) {
            switch (opcode) {
            case PUTSTATIC:
            case GETSTATIC:
            case PUTFIELD:
            case GETFIELD: break;
            default:
                throw new IllegalArgumentException(
                    "Illegal Field Access Instruction Opcode: " + opcode);
            }
            this.opcode = opcode;
            return this;
        }
    }
}
