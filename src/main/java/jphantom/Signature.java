package jphantom;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

public abstract class Signature implements Opcodes
{
    private String repr;
    private final int access;

    public Signature(int access) {
        this.access = access;
    }

    public boolean isAbstract() {
        return (access & ACC_ABSTRACT) != 0;
    }

    public boolean isPrivate() {
        return (access & ACC_PRIVATE) != 0;
    }
    
    public int getAccess() {
        return access;
    }

    protected String toString(Type t) {
        return t.getClassName().replaceFirst("java\\.lang\\.", "");
    }
    
    protected void appendAccess(StringBuilder buf)
    {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            buf.append("public ");
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            buf.append("private ");
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            buf.append("protected ");
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            buf.append("final ");
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            buf.append("static ");
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            buf.append("synchronized ");
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            buf.append("volatile ");
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            buf.append("transient ");
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            buf.append("abstract ");
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            buf.append("strictfp ");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            buf.append("synthetic ");
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            buf.append("enum ");
        }
    }

    public String toString()
    {
        // Lazy initialization
        if (repr != null) { return repr; }

        return repr = toStringAux();
    }

    public abstract String toStringAux();
}