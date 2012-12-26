package jphantom.access;

import java.util.*;
import java.lang.annotation.ElementType;
import util.Utils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import jphantom.constraints.*;

public class MethodAccessStateMachine extends AccessStateMachine<MethodAccessContext>
    implements Opcodes
{   
    private boolean accessed = false;
    private String desc;
    private final String methodName;
    private final Type owner;

    private MethodAccessStateMachine(String name, Type owner) {
        this.methodName = name;
        this.owner = owner;
    }

    @Override
    public MethodAccessStateMachine moveTo(MethodAccessContext ctx)
    {
        if (accessed && !desc.equals(ctx.desc))
            throw new IllegalStateException(
                "Method \'" + owner.getClassName() + " " + methodName +
                "\' has multiple descriptors: " + desc + " " + ctx.desc);

        switch (ctx.opcode) {
        case INVOKEVIRTUAL:
            addConstraint(new IsaClassConstraint(owner));
            break;
        case INVOKESPECIAL:
            // <clinit> is never called explicitly
            // => name must be <init> => implies a class owner
            addConstraint(new IsaClassConstraint(owner));
            break;
        case INVOKESTATIC:
            addConstraint(new IsaClassConstraint(owner));
            makeStatic();
            break;
        case INVOKEINTERFACE:
            addConstraint(new IsanInterfaceConstraint(owner));
            state.add(Modifier.ABSTRACT);
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
            assert modifier.appliesTo(ElementType.METHOD);

        return this;
    }
    
    private void makeStatic()
    {
        if (accessed && !state.contains(Modifier.STATIC))
            throw new IllegalStateException(
                "Method \'" + owner.getClassName() + " " + methodName + 
                "\' must consistently be called as a static method");

        state.add(Modifier.STATIC);
    }

    ///////////////// Factory Method ///////////////

    private static Map<String,MethodAccessStateMachine> machines = Utils.newMap();
    
    public static MethodAccessStateMachine getInstance(String methodName, Type owner, String desc)
    {
        String key = owner.getClassName() + ":" + methodName + " " + desc;

        if (!machines.containsKey(key))
            machines.put(key, new MethodAccessStateMachine(methodName, owner));

        return machines.get(key);
    }
}
