package org.clyze.jphantom;

import java.io.*;
import java.util.*;
import com.google.common.collect.ForwardingSet;
import org.objectweb.asm.Type;
import org.clyze.jphantom.methods.MethodLookupTable;

public class Phantoms extends ForwardingSet<Type>
{
    /////////////////// Singleton ///////////////////

    private static final Phantoms instance = new Phantoms();
    public static Phantoms V() { return instance; }

    ////////////////// Constructor //////////////////

    private Phantoms() {}

    ////////////////////  Fields ////////////////////

    private final Map<Type,Transformer> transformers = new HashMap<>();

    private final MethodLookupTable mtable = new MethodLookupTable();

    //////////////////// Methods ////////////////////

    @Override
    protected Set<Type> delegate() {
        return transformers.keySet();
    }

    public Transformer getTransformer(final Type type)
    {
        if (!transformers.containsKey(type))
        {
            Transformer tr = new Transformer(type);

            tr.top = mtable.new CachingAdapter(tr.top);
            transformers.put(type, tr);
        }
        return transformers.get(type);
    }

    public MethodLookupTable getLookupTable() {
        return mtable;
    }

    public List<File> generateFiles(File outDir) throws IOException
    {
        List<File> files = new LinkedList<>();

        for (Map.Entry<Type,Transformer> e : transformers.entrySet())
        {
            Type key = e.getKey();
            byte[] bytes = e.getValue().transform();

            // Dump the class in a file

            File outFile = locationOf(outDir, key);

            if (!outFile.getParentFile().isDirectory() && 
                !outFile.getParentFile().mkdirs())
                throw new IOException("" + outFile.getParentFile());

			try (DataOutputStream dout = new DataOutputStream(
					new FileOutputStream(outFile))) {
				dout.write(bytes);
				dout.flush();
			}
            files.add(outFile);
        }
        return files;
    }

    public static File locationOf(File outDir, Type type) {
        return new File(outDir, type.getClassName().replace('.', '/') + ".class");
    }
}
