package jphantom.methods;

import java.util.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import jphantom.access.*;
import jphantom.exc.IllegalBytecodeException;

public class MethodLookupTable extends HashMap<Type, Set<MethodSignature>> {
    protected static final long serialVersionUID = 834573459345L;
    protected int pendingAdapters = 0;

    public MethodLookupTable() {
        super();
    }

    // Copy constructor
    public MethodLookupTable(MethodLookupTable other) {
        super(other);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Type,Set<MethodSignature>> e : entrySet())
        {
            builder.append("Methods of: ").append(e.getKey()).append('\n');
            
            for (MethodSignature n : e.getValue())
                builder.append("   ").append(n).append("\n");
        }
            
        return builder.toString();
    }

    ////////////// Caching Adapter //////////////

    public class CachingAdapter extends ClassVisitor implements Opcodes
    {
        private Type clazz;

        public CachingAdapter(int api, ClassVisitor cv) {
            super(api, cv);
            pendingAdapters++;
        }

        public CachingAdapter(int api) {
            super(api);
            pendingAdapters++;
        }

        public CachingAdapter(ClassVisitor cv) {
            this(ASM4, cv);
        }

        public CachingAdapter() {
            this(ASM4);
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces)
        {
            // Enforce that the adapter will run just once
            if (clazz != null)
                throw new IllegalStateException();
            else
                clazz = Type.getObjectType(name);

            put(clazz, new HashSet<MethodSignature>());
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, 
                                         String signature, String[] exceptions)
        {
            try {
               get(clazz).add(
                  new MethodSignature.Builder(name, desc)
                  .access(access).exceptions(exceptions).build()
               );
            } catch (RuntimeException exc) {
                throw new IllegalBytecodeException.Builder(clazz)
                    .method(name, desc).cause(exc).build();
            }

            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            pendingAdapters--;
        }
    }
}
