package jphantom;

import java.util.*;
import java.io.*;
import java.util.jar.*;

import jphantom.tree.*;
import jphantom.tree.closure.*;
import jphantom.exc.PhantomLookupException;
import jphantom.fields.FieldSignature;
import jphantom.methods.MethodSignature;
import org.objectweb.asm.*;

public class ClassMembers implements Opcodes, Types
{
    private final Map<Type,Record> records = new HashMap<>();
    private final ClassHierarchy hierarchy;
    private final Random rand = new Random(System.currentTimeMillis());
    
    private ClassMembers(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public FieldSignature lookupField(Type clazz, String fieldName) 
    throws PhantomLookupException 
    {
        if (!records.containsKey(clazz))
            throw new IllegalArgumentException("" + clazz);

        return records.get(clazz).lookupField(fieldName);
    }

    public MethodSignature lookupMethod(Type clazz, String methodName, String methodDesc) 
    throws PhantomLookupException 
    {
        if (!records.containsKey(clazz))
            throw new IllegalArgumentException(clazz + " not contained in key set");

        return records.get(clazz).lookupMethod(methodName, methodDesc);
    }

    public MethodSignature lookupInterfaceMethod(Type clazz, String methodName, String methodDesc) 
    throws PhantomLookupException 
    {
        if (!records.containsKey(clazz))
            throw new IllegalArgumentException(clazz + " not contained in key set");

        return records.get(clazz).lookupIMethod(methodName, methodDesc);
    }

    private class Record {
        private final Type type;
        private final Map<String,FieldSignature> fields = new HashMap<>();
        private final Map<String,MethodSignature> methods = new HashMap<>();

        public Record(Type type) {
            this.type = type;
        }

        private String mkey(String name, String desc) {
            return name + "::" + desc;
        }

        protected void addField(String name, FieldSignature sign) {
            fields.put(name, sign);
        }

        protected void addMethod(String name, MethodSignature sign) {
            methods.put(mkey(name, sign.getDescriptor()), sign);
        }

        public FieldSignature lookupField(String name) throws PhantomLookupException
        {
            Record rec = this;

            while (rec != null) {
                if (rec.fields.containsKey(name))
                    return rec.fields.get(name);

                Type sc = hierarchy.getSuperclass(rec.type);
                
                if (!hierarchy.contains(sc))
                    throw new PhantomLookupException(sc);

                rec = records.get(sc);
            }
            return null;
        }

        public MethodSignature lookupMethod(String name, String desc)
        throws PhantomLookupException
        {
            Record rec = this;

            // Special case for interfaces
            if (hierarchy.isInterface(rec.type))
                return lookupIMethod(name, desc);

            // For classes look only in superclasses
            while (true) {
                String key = mkey(name, desc);

                if (rec.methods.containsKey(key))
                    return rec.methods.get(key);

                Type sc = hierarchy.getSuperclass(rec.type);
                
                if (sc == null) {
                    assert rec.type.equals(OBJECT);
                    break;
                }

                if (!hierarchy.contains(sc))
                    throw new PhantomLookupException(sc);

                rec = records.get(sc);
                assert rec != null : "Missing record for: " + sc;
            }
            return null;
        }

        private MethodSignature lookupIMethod(String name, String desc)
        throws PhantomLookupException
        {
            Collection<Type> ifaces;
            String key = mkey(name, desc);

            try {
                // Include OBJECT, since we may be searching for
                // one of its methods
                ifaces = new PseudoSnapshot(hierarchy).getAllSupertypes(type);
            } catch (IncompleteSupertypesException exc) {
                ifaces = exc.getSupertypes();
            }

            List<Type> phantoms = new LinkedList<>();

            // Check existing super-interfaces
            for (Type iface : ifaces) {
                // Add to phantoms
                if (!hierarchy.contains(iface)) {
                    phantoms.add(iface);
                    continue;
                }
                Record rec = records.get(iface);
                assert rec != null;

                if (rec.methods.containsKey(key))
                    return rec.methods.get(key);
            }

            // Randomize the remaining supertypes order
            Collections.shuffle(phantoms, rand);

            if (!phantoms.isEmpty())
                throw new PhantomLookupException(phantoms.get(0));

            return null;
        }
    }

    ////////////// Feeder //////////////

    public class Feeder extends ClassVisitor implements Opcodes
    {
        private Type clazz;

        public Feeder(int api, ClassVisitor cv) {
            super(api, cv);
        }

        public Feeder(int api) {
            super(api);
        }

        public Feeder(ClassVisitor cv) {
            this(ASM4, cv);
        }

        public Feeder() {
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
            clazz = Type.getObjectType(name);
            records.put(clazz, new Record(clazz));
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, 
                                         String signature, String[] exceptions)
        {
            MethodSignature sign = new MethodSignature.Builder(name, desc)
                .access(access).exceptions(exceptions).build();
            
            records.get(clazz).addMethod(name, sign);
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, 
                                         String signature, Object value)
        {
            FieldSignature sign = new FieldSignature.Builder(name, desc)
                .access(access).build();
            
            records.get(clazz).addField(name, sign);
            return super.visitField(access, name, desc, signature, value);
        }
    }

    /////////////// Factory Method ///////////////

    public static ClassMembers fromJar(String jarname, ClassHierarchy hierarchy) throws IOException
    {
        return fromJar(new JarFile(jarname), hierarchy);
    }

    public static ClassMembers fromJar(JarFile file, ClassHierarchy hierarchy) throws IOException
    {
        try {
            ClassMembers repo = new ClassMembers(hierarchy);
            
            new ClassReader(OBJECT.getInternalName()).accept(repo.new Feeder(), 0);

            for (Enumeration<JarEntry> e = file.entries(); e.hasMoreElements();)
            {
                JarEntry entry = e.nextElement();

                /* Skip directories */
                if (entry.isDirectory())
                    continue;
            
                /* Skip non-class files */
                if (!entry.getName().endsWith(".class"))
                    continue;

                InputStream stream = file.getInputStream(entry);

                try {
                    ClassReader reader = new ClassReader(stream);

                    // Add type to hierarchy
                    reader.accept(repo.new Feeder(), 0);
                } finally {
                    stream.close();
                }
            }
            return repo;
        } finally {
            file.close();
        }
    }
}
