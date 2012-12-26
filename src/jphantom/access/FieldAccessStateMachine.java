package jphantom.access;

import java.util.*;
import java.lang.annotation.ElementType;
import util.Utils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import jphantom.constraints.*;

public class FieldAccessStateMachine extends AccessStateMachine<FieldAccessContext>
    implements Opcodes
{
    private boolean accessed = false;
    private String desc;
    private final String fieldName;
    private final Type owner;

    private FieldAccessStateMachine(String name, Type owner) {
        this.fieldName = name;
        this.owner = owner;
    }

    @Override
    public FieldAccessStateMachine moveTo(FieldAccessContext ctx)
    {
        if (accessed && !desc.equals(ctx.desc))
            throw new IllegalStateException(
                "Field \'" + owner.getClassName() + " " + fieldName +
                "\' has multiple descriptors: " + desc + " " + ctx.desc);

        switch (ctx.opcode) {
        case PUTSTATIC:
            makeStatic();
            makeNonFinal();
            addConstraint(new IsaClassConstraint(owner));
            break;
        case GETSTATIC:
            makeStatic();
            makeFinal();
            break;
        case PUTFIELD:
            makeNonFinal();
            addConstraint(new IsaClassConstraint(owner));
            break;
        case GETFIELD: 
            makeFinal();
            addConstraint(new IsaClassConstraint(owner));
            break;
        default:
            throw new AssertionError();
        }
        accessed = true;
        desc = ctx.desc;
        state.add(Modifier.PUBLIC);

        // Sanity Checks
        assert !Modifier.hasConflict(state);
        for (Modifier modifier : state)
            assert modifier.appliesTo(ElementType.FIELD);

        return this;
    }
    
    private void makeStatic()
    {
        if (accessed && !state.contains(Modifier.STATIC))
            throw new IllegalStateException(
                "Field \'" + owner.getClassName() + " " + fieldName + 
                "\' must consistently be used as a static field");

        state.add(Modifier.STATIC);
    }

    private void makeFinal()
    {
        if (!accessed)
            state.add(Modifier.FINAL);
    }

    private void makeNonFinal()
    {
        state.remove(Modifier.FINAL);
    }

    ///////////////// Factory Method ///////////////

    private static Map<String,FieldAccessStateMachine> machines = Utils.newMap();
    
    public static FieldAccessStateMachine getInstance(String fieldName, Type owner)
    {
        String key = owner.getClassName() + ":" + fieldName;

        if (!machines.containsKey(key))
            machines.put(key, new FieldAccessStateMachine(fieldName, owner));

        return machines.get(key);
    }
}
