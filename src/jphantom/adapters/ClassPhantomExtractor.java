package jphantom.adapters;

import jphantom.Phantoms;
import jphantom.Transformer;
import jphantom.access.*;
import jphantom.tree.*;
import jphantom.exc.IllegalBytecodeException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.util.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.signature.*;

public class ClassPhantomExtractor extends ClassVisitor implements Opcodes
{
    private final Phantoms phantoms = Phantoms.V();
    private final ClassHierarchy hierarchy;
    private final SignatureVisitor sv;
    private Type clazz;
    private String mname;
    private String mdesc;

    public ClassPhantomExtractor(int api, ClassVisitor cv, ClassHierarchy hierarchy) {
        super(api, cv);
        this.hierarchy = hierarchy;
        this.sv = new PhantomAdder(hierarchy, phantoms);
    }

    public ClassPhantomExtractor(ClassVisitor cv, ClassHierarchy hierarchy) {
        this(ASM4, cv, hierarchy);
    }

    public ClassPhantomExtractor(ClassHierarchy hierarchy) {
        this(null, hierarchy);
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

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        if (exceptions != null) {
            for (String type : exceptions) {
                Type exc = Type.getObjectType(type);

                // Scan exception
                new SignatureReader(exc.toString()).acceptType(sv);
            }
        }
        mname = name;
        mdesc = desc;
        return new MethodPhantomExtractor(
            super.visitMethod(access, name, desc, signature, exceptions));
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
        public void visitMethodInsn(int opcode, String owner, String name, String desc)
        {

            do {
                Type phantom = Type.getObjectType(owner);

                // Scan descriptors for phantom types

                new SignatureReader(phantom.toString()).acceptType(sv);
                new SignatureReader(desc).accept(sv);

                if (phantom.getSort() == Type.ARRAY || hierarchy.contains(phantom))
                    break;

                // Get top class visitor

                assert phantoms.contains(phantom) : phantom;

                Transformer tr = phantoms.getTransformer(phantom);

                // Construct new method access context

                MethodAccessEvent event = new MethodAccessEvent.Builder()
                    .setOpcode(opcode)
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

            super.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            new SignatureReader(Type.getObjectType(type).toString()).acceptType(sv);
            super.visitTypeInsn(opcode, type);
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
        public void visitFieldInsn(int opcode, String owner, String name, String desc)
        {
            do {
                Type phantom = Type.getObjectType(owner); 

                // Scan descriptors for phantom types

                new SignatureReader(phantom.toString()).acceptType(sv);
                new SignatureReader(desc).acceptType(sv);

                // Skip available classes

                if (phantom.getSort() == Type.ARRAY || hierarchy.contains(phantom))
                    break;

                // Get top class visitor

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
    }
}
