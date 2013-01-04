package jphantom;

import util.Command;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.Opcodes;

public class Transformer implements Command, Opcodes
{
    private static final int options = 
        ClassWriter.COMPUTE_MAXS |
        ClassWriter.COMPUTE_FRAMES;

    private ClassWriter writer;
    private ClassReader reader;
    private byte[] bytes;
    public ClassVisitor top;

    protected Transformer(Type type)
    {
        ClassWriter cw = new ClassWriter(options);

        // Set initial flags
        cw.visit(V1_5, 
                 ACC_PUBLIC, 
                 type.getInternalName(), 
                 null, 
                 Type.getInternalName(Object.class), 
                 null
            );
        
        cw.visitEnd();

        // Field initialization

        bytes = cw.toByteArray();
        reader = new ClassReader(bytes);
        writer = new ClassWriter(reader, options);
        top = new CheckClassAdapter(writer);
    }

    @Override
    public void execute() {
        transform();
    }

    public byte[] transform() {
        reader.accept(top, ClassReader.EXPAND_FRAMES);
        return bytes = writer.toByteArray();
    }
}
