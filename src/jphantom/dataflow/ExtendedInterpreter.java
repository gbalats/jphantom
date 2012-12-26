package jphantom.dataflow;

import java.util.*;
import jphantom.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.tree.*;

import static util.Utils.*;
import static jphantom.dataflow.TypeInterpreter.NULL_VALUE;


public class ExtendedInterpreter extends Interpreter<CompoundValue> implements Opcodes
{
    private TypeInterpreter i;
    private ClassHierarchy hier;

    public ExtendedInterpreter(ClassHierarchy hier) {
        this(ASM4, hier);
    }

    public ExtendedInterpreter(int api, ClassHierarchy hier) {
        super(api);

        // Delegator

        this.i = new TypeInterpreter(api, hier);
        this.hier = hier;
    }

    @Override
    public CompoundValue newValue(Type type) {
        return CompoundValue.fromBasicValue(i.newValue(type));
    }

    @Override
    public CompoundValue newOperation(AbstractInsnNode insn) throws AnalyzerException
    {
        return CompoundValue.fromBasicValue(i.newOperation(insn));
    }

    @Override
    public CompoundValue copyOperation(AbstractInsnNode insn, CompoundValue value)
        throws AnalyzerException
    {
        return value;
    }

    @Override
    public CompoundValue unaryOperation(AbstractInsnNode insn, CompoundValue value)
        throws AnalyzerException
    {
        // Doesn't really need the argument value to compute the returned value
        return CompoundValue.fromBasicValue(i.unaryOperation(insn, value.asBasicValue()));
    }

    @Override
    public CompoundValue binaryOperation(
        AbstractInsnNode insn, 
        CompoundValue value1, 
        CompoundValue value2) throws AnalyzerException
    {
        BasicValue v1 = value1.asBasicValue(), v2 = value2.asBasicValue();

        if (insn.getOpcode() == AALOAD) {
            switch (value1.values().size()) {
            case 0:
            case 1: break;
            default:
                CompoundValue left = null;
                // Many values
                for (BasicValue b : value1.values()) {
                    // Iterate over basic values to get the element types
                    CompoundValue right = CompoundValue.fromBasicValue(
                        i.binaryOperation(insn, b, v2));
                    // Merge the element type
                    left = (left == null) ? right : merge(left, right);
                }
                return left;
            }
        }
        return CompoundValue.fromBasicValue(
            i.binaryOperation(insn, v1, v2));
    }

    @Override
    public CompoundValue ternaryOperation(
        AbstractInsnNode insn,
        CompoundValue value1,
        CompoundValue value2,
        CompoundValue value3) throws AnalyzerException
    {
        return CompoundValue.fromBasicValue(
            i.ternaryOperation(insn,
                               value1.asBasicValue(),
                               value2.asBasicValue(),
                               value3.asBasicValue())
            );
    }

    @Override
    public CompoundValue naryOperation(
        AbstractInsnNode insn,
        List< ? extends CompoundValue> values) throws AnalyzerException
    {
        List<BasicValue> l = newList();

        for (CompoundValue v : values)
            l.add(v.asBasicValue());

        // Doesn't really need the argument values to compute the returned value
        return CompoundValue.fromBasicValue(i.naryOperation(insn, l));
    }

    @Override
    public void returnOperation(
        AbstractInsnNode insn,
        CompoundValue value,
        CompoundValue expected) throws AnalyzerException
    {
        i.returnOperation(insn, value.asBasicValue(), expected.asBasicValue());
    }

    @Override
    public CompoundValue merge(CompoundValue v, CompoundValue w) {
        return CompoundValue.fromMerge(v,w);
    }
}
