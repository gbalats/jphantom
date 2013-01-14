package jphantom.constraints.extractors;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import jphantom.tree.*;
import jphantom.exc.*;
import jphantom.dataflow.*;
import jphantom.conversions.*;
import jphantom.constraints.solvers.*;
import jphantom.ArrayType;
import util.Command;

public class TypeConstraintExtractor extends AbstractExtractor 
    implements Opcodes
{
    private static final boolean DEBUG = false;

    private final Analyzer<CompoundValue> analyzer;
    private final Interpreter<CompoundValue> interpreter;
    private String cName;
    private Type returnType;
    private Map<Label,List<Command>> commands;
   
    public TypeConstraintExtractor(TypeConstraintSolver solver) {
        super(solver);
        this.interpreter = new ExtendedInterpreter(hierarchy);
        this.analyzer = new Analyzer<CompoundValue>(interpreter);
    }

    public final void visit(ClassNode node) throws AnalyzerException {
        cName = node.name;
        if (DEBUG)
            System.out.println("... analyzing " + cName);

        for (MethodNode meth : node.methods) {
            if (DEBUG)
                System.out.println("  ... Method: " + meth.name);
            visit(meth);
        }
    }

    public final void visit(MethodNode meth) throws AnalyzerException
    {
        // Save return type
        returnType = Type.getReturnType(meth.desc);

        // Analyze Method
        analyzer.analyze(cName, meth);

        MethodConstraintExtractor mv = new MethodConstraintExtractor();

        // Local Variable Map

        commands = new HashMap<>();

        if (meth.localVariables != null)
            for (LocalVariableNode local : meth.localVariables) {
                Type t = Type.getType(local.desc);
                int index = local.index;
                Label start = local.start.getLabel();
                Label end = local.end.getLabel();

                Command addition = mv.new LocalVariableAddition(index, t, local.name);
                Command removal = mv.new LocalVariableRemoval(index, t, local.name);

                if (!commands.containsKey(start))
                    commands.put(start, new LinkedList<Command>());

                if (!commands.containsKey(end))
                    commands.put(end, new LinkedList<Command>());

                commands.get(start).add(addition);
                commands.get(end).add(removal);
            }

        // Extract constraints

        try {
            meth.accept(mv);
        } catch (InsolvableConstraintException e) {
            System.err.println("Error while analyzing method \"" + meth.name + 
                               "\" of class \"" + cName + "\": insn " + mv.insnNo);
            if (DEBUG)
                for (int i = 0; i <= mv.insnNo; i++)
                    System.err.println("Frame at point:" + i + "\n" + analyzer.getFrames()[i]);
            throw e;
        }
    }

    public class MethodConstraintExtractor extends MethodVisitor
    {   
        private int insnNo = 0;
        private boolean inTable;
        private Map<CompoundValue,Integer> arrayIndices = new HashMap<>();
        private Map<Integer,Type> activeLocalVariables = new HashMap<>();

        // Constructor chaining

        public MethodConstraintExtractor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        public MethodConstraintExtractor(MethodVisitor mv) {
            this(ASM4, mv);
        }

        public MethodConstraintExtractor() {
            this(null);
        }

        private Frame<CompoundValue> getFrame() throws UnreachableCodeException
        {
            Frame<CompoundValue> frame = analyzer.getFrames()[insnNo];

            if (frame == null)
                throw new UnreachableCodeException();

            return frame;
        }

        private CompoundValue getStack(int i) throws UnreachableCodeException
        {
            int top = getFrame().getStackSize();
            return getFrame().getStack(top -1 -i);
        }

        
        private CompoundValue getLocal(int i) throws UnreachableCodeException
        {
            inTable = false;

            // Local Variable Table contains exact type
            if (activeLocalVariables.containsKey(i)) {
                inTable = true;
                return CompoundValue.fromBasicValue(
                    TypeInterpreter.getValue(
                        activeLocalVariables.get(i)
                    )
                );
            }
            int max = getFrame().getLocals();
            return i < max ? getFrame().getLocal(i) : null;
        }

        ////////////////////// Instructions //////////////////////

        @Override
        public void visitInsn(int opcode)
        {
            try {
                switch (opcode) {
                case ATHROW:
                    CompoundValue exc = getStack(0);
                    addConstraint(exc, THROWABLE);
                    // This constraint is enough since the requirement
                    // to explicitly declare any checked exceptions 
                    // does not apply at the bytecode level, but is 
                    // imposed by javac instead.
                    break;
                case ARETURN:
                    addConstraint(getStack(0), returnType);
                    break;
                case AASTORE:
                    do {
                        CompoundValue val = getStack(0);
                        CompoundValue arrayObj = getStack(2);
                        Type declaredType;
                    
                        if (arrayIndices.containsKey(arrayObj)) {
                            // Stored in local variable
                            declaredType = getLocal(arrayIndices.get(arrayObj)).
                                asBasicValue().getType();
                        } else if (arrayObj.asBasicValue() != null) {
                            // Single value
                            declaredType = arrayObj.asBasicValue().getType();
                        
                            if (declaredType == null)
                                break;

                            if (declaredType.equals(NULL_TYPE))
                                break;
                        } else { break; }
                    
                        // Elements must be of the appropriate type
                        addConstraint(val, ArrayType.elementOf(declaredType));
                    } while(false);
                    break;
                default:
                    break;
                }
            } catch (UnreachableCodeException ign) {}

            insnNo++;
            super.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            insnNo++;
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int var)
        {
            try {
                switch(opcode) {
                case ASTORE:
                    CompoundValue obj = getStack(0);
                    CompoundValue val = getLocal(var);

                    if (inTable) {
                        Type declaredType = val.asBasicValue().getType(); 

                        addConstraint(obj, declaredType);
                    
                        // Store array index
                        if (declaredType.getSort() == Type.ARRAY)
                            arrayIndices.put(obj, var);
                        else
                            arrayIndices.remove(obj);
                    }
                    break;
                default:
                    break;
                }
            } catch (UnreachableCodeException ign) {}

            insnNo++;
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            insnNo++;
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(
            int opcode, 
            String owner, 
            String name, 
            String desc)
        {
            CompoundValue obj, val;

            try {                
                switch (opcode) {
                case GETSTATIC:
                    break;
                case GETFIELD:
                    obj = getStack(0);
                    addConstraint(obj, Type.getObjectType(owner));
                    break;
                case PUTFIELD:
                    obj = getStack(1);
                    addConstraint(obj, Type.getObjectType(owner));
                case PUTSTATIC:
                    val = getStack(0);
                    addConstraint(val, Type.getType(desc));
                    break;
                default:
                    throw new AssertionError();
                }
            } catch (UnreachableCodeException ign) {}

            insnNo++;
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(
            int opcode, 
            String owner, 
            String name, 
            String desc)
        {
            try {
                Type[] args = Type.getArgumentTypes(desc);
                int pos = 0; // stack counter

                // Widening constraints for arguments in reverse order

                for (int i = args.length - 1; i >= 0; i--) {
                    Type formalArg = args[i];
                    CompoundValue actualArg = getStack(pos++);

                    // Add constraint
                    addConstraint(actualArg, formalArg);
                }

                // Widening constraint for receiver

                switch(opcode) {
                case INVOKESPECIAL:
                    if (!"<init>".equals(name))
                        break;
                case INVOKEVIRTUAL:
                case INVOKEINTERFACE:
                    CompoundValue receiver = getStack(pos);

                    // Add constraint for owner
                    addConstraint(receiver, Type.getObjectType(owner));
                case INVOKESTATIC:
                    break;
                default:
                    throw new AssertionError();
                }
            } catch (UnreachableCodeException ign) {}

            insnNo++;
            super.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitInvokeDynamicInsn(
            String name,
            String desc,
            Handle bsm,
            Object... bsmArgs)
        {
            insnNo++;
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            insnNo++;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(Label label) {
            if (commands.containsKey(label)) {
                for (Command command : commands.get(label))
                    command.execute();
                commands.remove(label);
            }
            insnNo++;
            super.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            insnNo++;
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment)
        {
            insnNo++;
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(
            int min,
            int max,
            Label dflt,
            Label... labels)
        {
            insnNo++;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(
            Label dflt,
            int[] keys,
            Label[] labels)
        {
            insnNo++;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims)
        {
            insnNo++;
            super.visitMultiANewArrayInsn(desc, dims);
        }

        @Override
        public void visitFrame(
            int type, 
            int nLocal, 
            Object[] local, 
            int nStack, 
            Object[] stack) 
        {
            insnNo++;
            super.visitFrame(type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            insnNo++;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
        {
            if (type != null)
                addConstraint(Type.getObjectType(type), THROWABLE);
            super.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public void visitCode() {
            if (analyzer.getFrames() == null)
                throw new IllegalStateException("frames have not been set");
            super.visitCode();
        }

        @Override
        public void visitEnd() {
            if (insnNo != analyzer.getFrames().length) {
                throw new IllegalArgumentException(
                    "Total Frames (" + analyzer.getFrames().length + 
                    ") != Number of Instructions (" + insnNo + ")");
            }
            // Sanity Check
            for (List<Command> commandSet : commands.values())
                assert commandSet.isEmpty();
            super.visitEnd();
        }

        protected void addConstraint(CompoundValue subtypes, Type supertype)
        {
            for (BasicValue val : subtypes.values()) {
                Type subtype = val.getType();

                addConstraint(subtype, supertype);
            }            
        }

        protected void addConstraint(Type subtype, Type supertype)
        {
            Conversions.getAssignmentConversion(subtype, supertype).
                accept(TypeConstraintExtractor.this);
        }

        ////////////////////// Commands //////////////////////

        public abstract class LocalVariableChange implements Command
        {
            protected final int index;
            protected final Type type;
            protected final String name;

            public LocalVariableChange(int index, Type type, String name) {
                this.index = index;
                this.type = type;
                this.name = name;
            }
        }

        public class LocalVariableAddition extends LocalVariableChange
        {
            public LocalVariableAddition(int index, Type type, String name) {
                super(index, type, name);
            }

            @Override
            public void execute() {
                assert !activeLocalVariables.containsKey(index) : name;
                activeLocalVariables.put(index, type);
            }
        }

        public class LocalVariableRemoval extends LocalVariableChange
        {
            public LocalVariableRemoval(int index, Type type, String name) {
                super(index, type, name);
            }

            @Override
            public void execute() {
                assert activeLocalVariables.containsKey(index) : name;
                assert activeLocalVariables.get(index).equals(type) : name;
                activeLocalVariables.remove(index);
            }
        }
    }

    private class UnreachableCodeException extends Exception
    {
        protected static final long serialVersionUID = 893453456345L;
    }
}
