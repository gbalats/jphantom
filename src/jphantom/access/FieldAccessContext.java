package jphantom.access;

import org.objectweb.asm.Opcodes;

public class FieldAccessContext implements AccessContext, Opcodes
{
    public final String methodName;
    public final String desc;
    public final int opcode;

    private FieldAccessContext(Builder builder) {
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

        public FieldAccessContext build() {
            return new FieldAccessContext(this);
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
