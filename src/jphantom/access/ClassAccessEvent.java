package jphantom.access;

public class ClassAccessEvent extends Event
{
    public final int access;

    private ClassAccessEvent(Builder builder) {
        super(builder.opcode);
        access = builder.access;
    }

    public String toString() {
        return "Accessing class as: " + access + 
            " " + super.toString();
    }

    public static class Builder
    {
        private int access = ACC_PUBLIC;
        private int opcode = NOP;

        public Builder() {}

        public ClassAccessEvent build() {
            return new ClassAccessEvent(this);
        }

        public Builder setAccess(int access) {
            this.access = access;
            return this;
        }

        public Builder setOpcode(int opcode) {
            switch (opcode) {
            default:
                throw new IllegalArgumentException(
                    "Illegal Class Access Instruction Opcode: " + opcode);
            }
            // this.opcode = opcode;
            // return this;
        }
    }
}
