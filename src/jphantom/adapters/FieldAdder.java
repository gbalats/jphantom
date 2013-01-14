package jphantom.adapters;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

public class FieldAdder extends ClassVisitor implements Opcodes
{
    private final int fAcc;
    private final String fName;
    private final String fDesc;
    private boolean isFieldPresent;

    public FieldAdder(ClassVisitor cv, int fAcc, String fName, String fDesc)
    {
        super(ASM4, cv);
        this.fAcc = fAcc;
        this.fName = fName;
        this.fDesc = fDesc;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value)
    {
        if (name.equals(fName))
            isFieldPresent = true;
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        if (!isFieldPresent && cv != null) {
            FieldVisitor fv = cv.visitField(
                fAcc, fName, fDesc, null, null);
            if (fv != null)
                fv.visitEnd();
        }
        super.visitEnd();
    }
}
