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

    public Map<Type, byte[]> generateClasses() throws IOException
    {
        Map<Type, byte[]> map = new HashMap<>();

        for (Map.Entry<Type,Transformer> e : transformers.entrySet())
        {
            Type key = e.getKey();
            byte[] bytes = e.getValue().transform();

            map.put(key, bytes);
        }
        return map;
    }
}
