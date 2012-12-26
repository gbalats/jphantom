package jphantom.access;

import org.objectweb.asm.Opcodes;

public class MethodAccessContext implements AccessContext, Opcodes
{
    public final String methodName;
    public final String desc;
    public final int opcode;

    private MethodAccessContext(Builder builder) {
        methodName = builder.mName;
        opcode = builder.opcode;
        desc = builder.desc;
    }

    public static class Builder
    {
        private String mName = null;
        private String desc = null;
        private int opcode = NOP;

        public Builder() {}

        public MethodAccessContext build() {
            return new MethodAccessContext(this);
        }

        public Builder setMethod(String name) {
            this.mName = name;
            return this;
        }

        public Builder setDesc(String desc) {
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
