package jphantom.methods;

import util.Pair;
import org.objectweb.asm.Type;

public class ConflictingMethodException extends Exception
{
    protected static final long serialVersionUID = 83463458345L;

    public ConflictingMethodException(Type a, Type b) {
        super(a + " conflicts with " + b);
    }

    public ConflictingMethodException(
        Pair<Type,MethodSignature> a,
        Pair<Type,MethodSignature> b
    )
    {
        super(a.fst + ": " + a.snd + " conflicts with " + 
              b.fst + ": " + b.snd);
    }
}
