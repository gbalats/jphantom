package jphantom.constraints.extractors;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jphantom.hier.*;
import jphantom.exc.*;
import jphantom.dataflow.*;
import jphantom.conversions.*;
import jphantom.constraints.solvers.*;
import jphantom.ArrayType;
import util.Command;
import static org.objectweb.asm.util.Printer.OPCODES;
import static org.objectweb.asm.tree.analysis.BasicValue.UNINITIALIZED_VALUE;

public class TypeConstraintExtractor extends AbstractExtractor 
    implements Opcodes
{
    private final static Logger logger = 
        LoggerFactory.getLogger(TypeConstraintExtractor.class);

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
        logger.debug("... analyzing class {}", cName);

        for (MethodNode meth : node.methods) {
            logger.debug("  ... Method: {} {}", meth.name, meth.desc);
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
        } catch (Throwable e) {

            if (logger.isTraceEnabled())
                for (int i = 0; i <= mv.insnNo; i++)
                    logger.trace("Frame at point: {}\n{}", i, analyzer.getFrames()[i]);
            throw new IllegalBytecodeException.Builder(cName)
                .message("Instruction: %d", mv.insnNo)
                .method(meth.name, meth.desc).cause(e).build();
        }
    }

    public class MethodConstraintExtractor extends MethodVisitor
    {   
        private int insnNo = 0;
        private Map<Integer,Type> declarations = new HashMap<>();

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

        private void logInstruction(int opcode) {
            logInstruction(OPCODES[opcode]);
        }

        private void logInstruction(String instr) {
            logger.trace("Instruction ({}): {} ", instr, insnNo);
        }


        private Frame<CompoundValue> getFrame() throws UnreachableCodeException
        {
            Frame<CompoundValue> frame = analyzer.getFrames()[insnNo];

            if (frame == null)
                throw new UnreachableCodeException();

            return frame;
        }

        /** Returns the i-th element of the stack at this point, in 
          * reverse order. That is, {@code getStack(0)} will return
          * the last element that was inserted in the stack. 
          */
        private CompoundValue getStack(int i) throws UnreachableCodeException
        {
            int top = getFrame().getStackSize();
            return getFrame().getStack(top -1 -i);
        }

        private CompoundValue getLocal(int i)
            throws UnreachableCodeException
        {
            int max = getFrame().getLocals();
            return i < max ? getFrame().getLocal(i) : null;
        }


        ////////////////////// Instructions //////////////////////

        @Override
        public void visitInsn(int opcode)
        {
            logInstruction(opcode);
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
                    CompoundValue val = getStack(0);
                    CompoundValue arrayObj = getStack(2);

                    int max = getFrame().getLocals();

                    for (int i = 0; i < max; i++) {
                        if (arrayObj != getFrame().getLocal(i))
                            continue;
                        
                        if (declarations.containsKey(i)) {
                            Type declaredType = declarations.get(i);

                            assert declaredType != null;

                            if (declaredType.getSort() != Type.ARRAY) {
                                assert ARRAY_INTERFACES.contains(declaredType) ||
                                    declaredType.equals(OBJECT) : declaredType;
                                break;
                            }

                            // Elements must be of the appropriate type
                            addConstraint(val, ArrayType.elementOf(declaredType));
                            break;
                        }
                    }
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
            logInstruction(opcode);
            insnNo++;
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int var)
        {
            logInstruction(opcode);
            try {
                switch(opcode) {
                case ALOAD:
                    if (declarations.containsKey(var))
                    {
                        CompoundValue val = getLocal(var);
                        Type declaredType = declarations.get(var);
                        assert declaredType != null;

                        // Found local variable in local variable table
                        addConstraint(val, declaredType);
                    }
                    break;
                case ASTORE:
                    CompoundValue val = getLocal(var);
                    CompoundValue obj = getStack(0);

                    if (declarations.containsKey(var))
                    {
                        Type declaredType = declarations.get(var);
                        assert declaredType != null;

                        // Found local variable in local variable table
                        addConstraint(obj, declaredType);
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
            logInstruction(opcode);
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
            logInstruction(opcode);

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
            logInstruction(opcode);
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
            logInstruction(INVOKEDYNAMIC);
            insnNo++;
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            logInstruction(opcode);
            insnNo++;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(Label label) {
            if (commands.containsKey(label)) {
                for (Command command : commands.get(label)) {
                    command.execute();
                    logger.trace("Label {}: {}", insnNo, command);
                }
                commands.remove(label);
            }

            // A new entry in the local variable table is added exactly
            // AFTER the corresponding ASTORE instruction (if such exists).
            // Thus, we cannot extract the relevant type constraint at the 
            // point of the ASTORE instruction, since there will be no entry
            // in the local variable table. We could also try to trace the
            // constraint at this point (when the declared type is added to 
            // the local variable table), but this wouldn't always work.
            // The problem is that at the start of the local variable table
            // entry's region, the local variable slot may be inhabited by
            // a leftover item from another execution path. However, this 
            // item will be erased later by another ASTORE instruction before
            // any ALOAD loads it to the stack. Therefore, the correct point 
            // to extract the constraint is the point of the ALOAD instruction,
            // where the slot item must surely be of the declared type. 

            insnNo++;
            super.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            logInstruction(LDC);
            insnNo++;
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment)
        {
            logInstruction(IINC);
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
            logInstruction(TABLESWITCH);
            insnNo++;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(
            Label dflt,
            int[] keys,
            Label[] labels)
        {
            logInstruction(LOOKUPSWITCH);
            insnNo++;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims)
        {
            logInstruction(MULTIANEWARRAY);
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
            logInstruction("frame");
            insnNo++;
            super.visitFrame(type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            logInstruction("line");
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
            // Sanity Checks
            for (List<Command> commandSet : commands.values())
                assert commandSet.isEmpty();

            assert declarations.isEmpty();

            super.visitEnd();
        }

        protected void addConstraint(CompoundValue subtypes, Type supertype)
        {
            for (BasicValue subtype : subtypes.values())
                addConstraint(subtype, supertype);
        }

        protected void addConstraint(BasicValue subtype, Type supertype)
        {
            if (!subtype.equals(UNINITIALIZED_VALUE))
                addConstraint(subtype.getType(), supertype);
        }

        protected void addConstraint(Type subtype, Type supertype)
        {
            if (subtype == null)
                throw new IllegalArgumentException("Null subtype");

            if (supertype == null)
                throw new IllegalArgumentException("Null supertype");

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
                assert !declarations.containsKey(index) : name;
                declarations.put(index, type);
            }

            @Override
            public String toString() {
                return "Adding local variable: " + 
                    type.getClassName() + " " + name + 
                    " at slot " + index;
            }
        }

        public class LocalVariableRemoval extends LocalVariableChange
        {
            public LocalVariableRemoval(int index, Type type, String name) {
                super(index, type, name);
            }

            @Override
            public void execute() {
                assert declarations.containsKey(index) : name;
                assert declarations.get(index).equals(type) : name;
                declarations.remove(index);
            }

            @Override
            public String toString() {
                return "Removing local variable: " + 
                    type.getClassName() + " " + name + 
                    " at slot " + index;
            }
        }
    }

    private class UnreachableCodeException extends Exception
    {
        protected static final long serialVersionUID = 893453456345L;
    }
}
