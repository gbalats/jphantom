package jphantom;

import java.io.*;
import java.util.*;
import util.ForwardingMap;
import util.Utils;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import jphantom.methods.MethodLookupTable;

public class Phantoms extends ForwardingMap<Type,ClassVisitor> implements Opcodes
{
    /////////////////// Singleton ///////////////////

    private static final Phantoms instance = new Phantoms();
    public static Phantoms V() { return instance; }

    ////////////////// Constructor //////////////////

    private Phantoms() { super(Utils.<Type,ClassVisitor>newMap()); }

    ////////////////////  Fields ////////////////////

    private Map<Type,ClassWriter> writers = Utils.newMap();
    final MethodLookupTable mtable = new MethodLookupTable(); // TODO: private

    //////////////////// Methods ////////////////////

    @Override 
    public ClassVisitor put(Type key, ClassVisitor value) {
        if (!containsKey(key)) {
            if (!ClassWriter.class.isInstance(value))
                throw new IllegalArgumentException(
                    "Type \'" + key + 
                    "\': Initial visitor must be a ClassWriter instance");

            // Store the first visitor, who must be a ClassWriter instance
            writers.put(key, ClassWriter.class.cast(value));
        }
        return super.put(key, value);
    }

    public ClassVisitor putDefault(Type key) {
        if (containsKey(key))
            throw new IllegalStateException(key + " is already mapped to a value");

        ClassWriter cw = new ClassWriter(
            ClassWriter.COMPUTE_MAXS |
            ClassWriter.COMPUTE_FRAMES);

        // Set initial flags
        cw.visit(V1_5, 
                 ACC_PUBLIC, 
                 key.getInternalName(), 
                 null, 
                 Type.getInternalName(Object.class), 
                 null
            );
        
        cw.visitEnd();
        put(key, cw);
        put(key, mtable.new CachingAdapter(cw));
        return null;
    }

    public ClassVisitor getDefault(Type phantom)
    {
        if (!containsKey(phantom))
            putDefault(phantom);
        return get(phantom);
    }

    public List<File> generateFiles(File outDir) throws IOException
    {
        List<File> files = Utils.newList();

        for (Map.Entry<Type,ClassVisitor> e : entrySet())
        {
            Type key = e.getKey();
            ClassVisitor top = e.getValue();
            ClassWriter cw = writers.get(key);
            ClassReader cr = new ClassReader(cw.toByteArray());
            cr.accept(top, 0);

            // Dump the class in a file

            File outFile = locationOf(outDir, key);
            outFile.getParentFile().mkdirs();
            DataOutputStream dout = new DataOutputStream(new FileOutputStream(outFile));
            try {
                dout.write(cw.toByteArray());
                dout.flush();
            } finally {
                dout.close();
            }
            files.add(outFile);
        }
        return files;
    }

    public static File locationOf(File outDir, Type type) {
        return new File(outDir, type.getClassName().replace('.', '/') + ".class");
    }
}
