package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.clyze.jphantom.Phantoms;
import org.clyze.jphantom.Transformer;
import org.clyze.jphantom.ClassMembers;
import org.clyze.jphantom.fields.FieldSignature;
import org.clyze.jphantom.methods.MethodSignature;
import org.clyze.jphantom.access.*;
import org.clyze.jphantom.hier.*;
import org.clyze.jphantom.hier.closure.*;
import org.clyze.jphantom.exc.IllegalBytecodeException;
import org.clyze.jphantom.exc.PhantomLookupException;

import org.objectweb.asm.TypePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.signature.*;

public class ClassPhantomExtractor extends ClassVisitor implements Opcodes
{
    protected final static Logger logger = LoggerFactory.getLogger(ClassPhantomExtractor.class);

    private final Phantoms phantoms = Phantoms.V();
    private final ClassHierarchy hierarchy;
    private final ClassMembers members;
    private final SignatureVisitor sv;
    private Type clazz;
    private String mname;
    private String mdesc;

    public ClassPhantomExtractor(int api, ClassVisitor cv, ClassHierarchy hierarchy, ClassMembers members) {
        super(api, cv);
        this.hierarchy = hierarchy;
        this.members = members;
        this.sv = new PhantomAdder(hierarchy, members, phantoms);
    }

    public ClassPhantomExtractor(ClassVisitor cv, ClassHierarchy hierarchy, ClassMembers members) {
        this(Options.ASM_VER, cv, hierarchy, members);
    }

    public ClassPhantomExtractor(ClassHierarchy hierarchy, ClassMembers members) {
        this(null, hierarchy, members);
    }

    private boolean hasPhantomSupertype(Type type)
    {
        if (!hierarchy.contains(type))
            throw new IllegalArgumentException();

        // Search for all supertypes
        try {
            new PseudoSnapshot(hierarchy).getAllSupertypes(type);
        } catch (IncompleteSupertypesException exc) {
            // Phantom supertype exists => load every supertype
            for (Type i : exc.getSupertypes())
                new SignatureReader("" + i).acceptType(sv);
            return true;
        }
        return false;
    }

    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
        if (superName != null) {
            Type sc = Type.getObjectType(superName);

            new SignatureReader("" + sc).acceptType(sv);
        }

        if (interfaces != null)
            for (String i : interfaces) {
                Type iface = Type.getObjectType(i);

                new SignatureReader("" + iface).acceptType(sv);
            }

        clazz = Type.getObjectType(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private void visitAnnotationClass(Type klass)
    {
        new SignatureReader(klass.toString()).acceptType(sv);

        if (!hierarchy.contains(klass)) {

            assert phantoms.contains(klass) : klass;
        
            Transformer tr = phantoms.getTransformer(klass);
            ClassAccessEvent event = ClassAccessEvent.IS_ANNOTATION;

            int access = ClassAccessStateMachine.v()
                .getEventSequence(klass).moveTo(event).getCurrentAccess();

            // Chain an access adapter
            assert tr.top != null;
            tr.top = new AccessAdapter(tr.top, access);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new AnnotationPhantomExtractor(desc, super.visitAnnotation(desc, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return new AnnotationPhantomExtractor(desc, super.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    @Override
    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        Type phantom = Type.getType(desc);
        new SignatureReader(phantom.toString()).acceptType(sv);
        
        return new FieldPhantomExtractor(
            super.visitField(access, name, desc, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        // Storing method being analyzed
        mname = name;
        mdesc = desc;

        // Searching declared exceptions for missing types
        if (exceptions != null) {
            for (String type : exceptions) {
                Type exc = Type.getObjectType(type);

                // Scan exception
                new SignatureReader(exc.toString()).acceptType(sv);
            }
        }

        // Searching method descriptor for missing types
        new SignatureReader(desc).accept(sv);

        // Searching method body for missing types
        return new MethodPhantomExtractor(
            super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public void visitInnerClass(
        final String name, 
        final String outerName, 
        final String innerName, 
        final int access)
    {
        Type inner = Type.getObjectType(name);

        new SignatureReader("" + inner).acceptType(sv);

        if (outerName != null)
            new SignatureReader("" + Type.getObjectType(outerName)).acceptType(sv);

        do {
            if (hierarchy.contains(inner))
                break;

            assert phantoms.contains(inner) : inner;

            Transformer tr = phantoms.getTransformer(inner);

            // ClassAccessEvent event = new ClassAccessEvent.Builder()
            //     .setAccess(access)
            //     .build();

            // ClassAccessStateMachine.v()
            //     .getEventSequence(inner).moveTo(event).getCurrentAccess();

            // Chain an access adapter

            assert tr.top != null;
            // tr.top = new AccessAdapter(tr.top, access);

            // inner class attributes are not checked to be
            // consistent with the corresponding class file

        } while(false);
    }

    private class AnnotationPhantomExtractor extends AnnotationVisitor {
       private final Type phantom;

        public AnnotationPhantomExtractor(String desc, AnnotationVisitor annotationVisitor) {
            super(ClassPhantomExtractor.this.api, annotationVisitor);
            phantom = Type.getType(desc);
            visitAnnotationClass(phantom);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return new AnnotationPhantomExtractor(desc, super.visitAnnotation(name, desc));
        }

        @Override
        public void visit(String name, Object value) {
            do {
                Type phantom = this.phantom;

                // ASM doesn't pass the actual type of the thing being visited... ugh...
                Class<?> cls = value.getClass();
                String desc = "()" + Type.getType(cls).getDescriptor();

                // Skip available classes, except in the case of phantom field
                if (hierarchy.contains(phantom)) {
                    try {
                        // Lookup Method
                        MethodSignature sign = members.lookupInterfaceMethod(phantom, name, desc);
                        if (sign == null)
                            sign = lookupBackup(phantom, name, cls);
                        if (sign == null)
                            throw new IllegalBytecodeException.Builder(clazz)
                                    .message("Annotation method Lookup failed (%s): %s %s", phantom, desc, name)
                                    .build();
                        break;
                    } catch (PhantomLookupException exc) {
                        logger.trace("Found missing method reference in {}: {} {}", phantom, desc, name);
                        logger.trace("First supertype: {}", exc.missingClass());

                        // Add field to first phantom supertype instead
                        phantom = exc.missingClass();
                        assert phantom != null;
                    }
                }

                // Get top class visitor

                assert phantoms.contains(phantom) : phantom;

                Transformer tr = phantoms.getTransformer(phantom);

                // Construct new method access context

                MethodAccessEvent event = new MethodAccessEvent.Builder()
                        .setOpcode(INVOKEINTERFACE)
                        .setDescriptor(desc)
                        .setName(name)
                        .build();

                try {
                    // Compute new method access using the state machine

                    int access = MethodAccessStateMachine.v()
                            .getEventSequence(name, phantom, desc).moveTo(event).getCurrentAccess();

                    // Chain a method-adder adapter

                    assert tr.top != null;
                    tr.top = new MethodAdder(tr.top, access, name, desc);

                } catch(IllegalTransitionException exc) {

                    throw new IllegalBytecodeException.Builder(clazz)
                            .method(mname, mdesc).cause(exc).build();
                }
            } while(false);
            super.visit(name, value);
        }

        private MethodSignature lookupBackup(Type phantom, String name, Class<?> cls) throws PhantomLookupException {
            // This mess exists because ASM passes us an object and no actual info on what the original type of that object is.
            if (cls.equals(Type.class))
                cls = Class.class;
            else if (cls.equals(Byte.class))
                cls = byte.class;
            else if (cls.equals(Boolean.class))
                cls = boolean.class;
            else if (cls.equals(Character.class))
                cls = char.class;
            else if (cls.equals(Short.class))
                cls = short.class;
            else if (cls.equals(Integer.class))
                cls = int.class;
            else if (cls.equals(Long.class))
                cls = long.class;
            else if (cls.equals(Float.class))
                cls = float.class;
            else if (cls.equals(Double.class))
                cls = double.class;
            String desc = "()" + Type.getType(cls).getDescriptor();
            return members.lookupInterfaceMethod(phantom, name, desc);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            super.visitEnum(name, descriptor, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return super.visitArray(name);
        }
    }

    private class FieldPhantomExtractor extends FieldVisitor
    {
        public FieldPhantomExtractor() {
            super(ClassPhantomExtractor.this.api);
        }

        public FieldPhantomExtractor(FieldVisitor fv) {
            super(ClassPhantomExtractor.this.api, fv);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationPhantomExtractor(desc, super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationPhantomExtractor(desc, super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }
    }

    private class MethodPhantomExtractor extends MethodVisitor
    {
        public MethodPhantomExtractor() {
            super(ClassPhantomExtractor.this.api);
        }

        public MethodPhantomExtractor(MethodVisitor mv) {
            super(ClassPhantomExtractor.this.api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
        {

            do {
                Type phantom = Type.getObjectType(owner);

                // Scan descriptors for phantom types

                new SignatureReader(phantom.toString()).acceptType(sv);
                new SignatureReader(desc).accept(sv);

                // Skip array classes

                if (phantom.getSort() == Type.ARRAY)
                    break;

                boolean abstractMethod = itf;

                // Skip available classes, except in the case of phantom field

                if (hierarchy.contains(phantom)) {
                    try {
                        // No phantom supertypes, skip
                        if (!hasPhantomSupertype(phantom))
                            break;

                        // Lookup Method
                        MethodSignature sign = members.lookupMethod(phantom, name, desc);

                        // Lookup failed => Abstract class calling abstract method 
                        // possibly defined in an interface (TODO: check)
                        if (sign == null) {
                            // Mark as abstract method
                            abstractMethod = true;

                            // Search for referenced method in interfaces
                            sign = members.lookupInterfaceMethod(phantom, name, desc);

                            if (sign == null)
                                throw new IllegalBytecodeException.Builder(clazz)
                                    .method(mname, mdesc)
                                    .message("Method Lookup failed (%s): %s %s", phantom, desc, name)
                                    .build();
                        }
                        else if (!sign.getDescriptor().equals(desc))
                            throw new IllegalBytecodeException.Builder(clazz)
                                .method(mname, mdesc)
                                .message("Descriptors differ: %s != %s", desc, sign.getDescriptor())
                                .build();

                        break;
                    } catch (PhantomLookupException exc) {
                        logger.trace("Found missing method reference in {}: {} {}", phantom, desc, name);
                        logger.trace("First supertype: {}", exc.missingClass());

                        // Add field to first phantom supertype instead
                        phantom = exc.missingClass();
                        assert phantom != null;
                    }
                }

                // Get top class visitor

                if (!Options.V().isSoftFail())
                    assert phantoms.contains(phantom) : phantom;

                Transformer tr = phantoms.getTransformer(phantom);

                // Construct new method access context

                MethodAccessEvent eventMAcc = new MethodAccessEvent.Builder()
                    .setOpcode(abstractMethod ? INVOKEINTERFACE : opcode)
                    .setDescriptor(desc)
                    .setName(name)
                    .build();

                // Mark owner class as interface
                if (itf) {
                    ClassAccessEvent eventItf = ClassAccessEvent.IS_INTERFACE;

                    int access = ClassAccessStateMachine.v()
                            .getEventSequence(phantom).moveTo(eventItf).getCurrentAccess();

                    // Chain an access adapter
                    assert tr.top != null;
                    tr.top = new AccessAdapter(tr.top, access);
                }

                try {
                    // Compute new method access using the state machine

                    int access = MethodAccessStateMachine.v()
                        .getEventSequence(name, phantom, desc).moveTo(eventMAcc).getCurrentAccess();

                    // Chain a method-adder adapter

                    assert tr.top != null;
                    tr.top = new MethodAdder(tr.top, access, name, desc);

                } catch(IllegalTransitionException exc) {

                    throw new IllegalBytecodeException.Builder(clazz)
                        .method(mname, mdesc).cause(exc).build();
                }
            } while(false);

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            new SignatureReader(Type.getObjectType(type).toString()).acceptType(sv);
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                new SignatureReader(((Type) cst).toString()).acceptType(sv);
            }
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            new SignatureReader(desc).acceptType(sv);
            super.visitMultiANewArrayInsn(desc, dims);
        }

        @Override
        public void visitLocalVariable(
            String name, String desc, String signature, 
            Label start, Label end, int index)
        {
            new SignatureReader(desc).acceptType(sv);
            super.visitLocalVariable(name, desc, signature, start, end, index);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
        {
            if (type != null) 
            {
                Type exc = Type.getObjectType(type);

                // Scan exception
                new SignatureReader(exc.toString()).acceptType(sv);
            }
            super.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationPhantomExtractor(desc, super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationPhantomExtractor(desc, super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
            int parameter, String desc, boolean visible) {
            return new AnnotationPhantomExtractor(desc, super.visitParameterAnnotation(parameter, desc, visible));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc)
        {
            do {
                Type phantom = Type.getObjectType(owner); 

                // Scan descriptors for phantom types

                new SignatureReader(phantom.toString()).acceptType(sv);
                new SignatureReader(desc).acceptType(sv);

                // Skip array classes

                if (phantom.getSort() == Type.ARRAY)
                        break;

                // Skip available classes, except in the case of phantom field

                if (hierarchy.contains(phantom)) {
                    try {
                        // No phantom supertypes, skip
                        if (!hasPhantomSupertype(phantom))
                            break;

                        // Lookup either in superclasses, or in both class and
                        // interface supertypes in case of a static field.
                        FieldSignature sign = (opcode == GETSTATIC || opcode == PUTSTATIC)
                            ? members.lookupStaticField(phantom, name)
                            : members.lookupField(phantom, name);

                        // Lookup failed and no phantom supertypes were found
                        if (sign == null)
                            throw new IllegalBytecodeException.Builder(clazz)
                                .method(mname, mdesc)
                                .message("Field Lookup failed (%s): %s %s", phantom, desc, name)
                                .build();

                        // Check descriptor
                        if (!sign.getDescriptor().equals(desc)) {
                            // If the descriptor mismatch can be chalked up to inheritance, do not throw
                            // Otherwise, cannot resolve difference
                            if (!isSubtypeOf(sign.getType(), Type.getType(desc)))
                                throw new IllegalBytecodeException.Builder(clazz)
                                        .method(mname, mdesc)
                                        .message("Descriptors differ: %s != %s", desc, sign.getDescriptor())
                                        .build();
                        }
                        // Break so we don't attempt to generate something we already have in the hierarchy
                        break;
                    } catch (PhantomLookupException exc) {
                        logger.trace("Found missing field reference in {}: {} {}", phantom, desc, name);

                        // Add field to first phantom supertype instead
                        phantom = exc.missingClass();
                    }
                }

                // Get top class visitor
                if (!Options.V().isSoftFail())
                    assert phantoms.contains(phantom) : phantom;

                Transformer tr = phantoms.getTransformer(phantom);

                // Construct new field access context

                FieldAccessEvent event = new FieldAccessEvent.Builder()
                    .setOpcode(opcode)
                    .setDescriptor(desc)
                    .setName(name)
                    .build();

                try {
                    // Compute new field access using the state machine

                    int access = FieldAccessStateMachine.v()
                        .getEventSequence(name, phantom).moveTo(event).getCurrentAccess();

                    // Chain a field-adder adapter

                    assert tr.top != null;
                    tr.top = new FieldAdder(tr.top, access, name, desc);

                } catch(IllegalTransitionException exc) {

                    throw new IllegalBytecodeException.Builder(clazz)
                        .method(mname, mdesc).cause(exc).build();
                }
            } while(false);

            super.visitFieldInsn(opcode, owner, name, desc);
        }

        private boolean isSubtypeOf(Type type, Type supertype) {
            try {
                if (isSubtypeOf(hierarchy.getSuperclass(type), supertype))
                    for (Type itf : hierarchy.getInterfaces(type))
                        if (isSubtypeOf(itf, supertype))
                            return true;
            } catch (Throwable ignored) {
            }
            return false;
        }
    }
}
