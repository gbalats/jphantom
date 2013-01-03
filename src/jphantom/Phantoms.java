package jphantom;

import java.io.*;
import java.util.*;
import util.ForwardingMap;
import org.objectweb.asm.Type;
import jphantom.methods.MethodLookupTable;

public class Phantoms extends ForwardingMap<Type,Transformer>
{
    /////////////////// Singleton ///////////////////

    private static final Phantoms instance = new Phantoms();
    public static Phantoms V() { return instance; }

    ////////////////// Constructor //////////////////

    private Phantoms() {
        super(new HashMap<Type,Transformer>());
    }

    ////////////////////  Fields ////////////////////

    final MethodLookupTable mtable = new MethodLookupTable(); // TODO: private

    //////////////////// Methods ////////////////////

    public Transformer putDefault(Type key)
    {
        if (containsKey(key))
            throw new IllegalStateException(key + " is already mapped to a value");

        Transformer tr = new Transformer(key);
        tr.top = mtable.new CachingAdapter(tr.top);
        put(key, tr);
        return null;
    }

    public List<File> generateFiles(File outDir) throws IOException
    {
        List<File> files = new LinkedList<>();

        for (Map.Entry<Type,Transformer> e : entrySet())
        {
            Type key = e.getKey();
            byte[] bytes = e.getValue().transform();

            // Dump the class in a file

            File outFile = locationOf(outDir, key);
            outFile.getParentFile().mkdirs();
            DataOutputStream dout = new DataOutputStream(new FileOutputStream(outFile));
            try {
                dout.write(bytes);
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
