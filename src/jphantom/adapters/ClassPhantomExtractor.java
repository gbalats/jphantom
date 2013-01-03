package jphantom.adapters;

import jphantom.Phantoms;
import jphantom.Transformer;
import jphantom.access.*;
import jphantom.tree.*;

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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.signature.*;

public class ClassPhantomExtractor extends ClassVisitor implements Opcodes
{
    private final Phantoms phantoms = Phantoms.V();
    private final ClassHierarchy hierarchy;
    private final SignatureVisitor sv;

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

                Transformer tr = phantoms.get(phantom);

                // Construct new method access context

                MethodAccessContext ctx = new MethodAccessContext.Builder()
                    .setOpcode(opcode).setDesc(desc).build();

                // Compute new method access using the state machine

                int access = MethodAccessStateMachine
                    .getInstance(name, phantom, desc).moveTo(ctx).getCurrentAccess();

                // Chain a method-adder adapter

                MethodNode method = new MethodNode(access, name, desc, null, null);
                // TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
                assert tr.top != null;
                tr.top = new MethodAdder(tr.top, method);

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

                Transformer tr = phantoms.get(phantom);

                // Construct new field access context

                FieldAccessContext ctx = new FieldAccessContext.Builder()
                    .setOpcode(opcode).setDesc(desc).build();

                // Compute new field access using the state machine

                int access = FieldAccessStateMachine
                    .getInstance(name, phantom).moveTo(ctx).getCurrentAccess();

                // Chain a field-adder adapter

                FieldNode field = new FieldNode(access, name, desc, null, null);
                // TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
                assert tr.top != null;
                tr.top = new FieldAdder(tr.top, field);
            } while(false);

            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}
