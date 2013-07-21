package jphantom.exc;

import org.objectweb.asm.Type;

public class PhantomLookupException extends Exception
{
    protected static final long serialVersionUID = 8934652376352346345L;
    public final Type phantom;

    public PhantomLookupException(Type phantom) {
        super();
        this.phantom = phantom;
    }

    public Type missingClass() {
        return phantom;
    }
}
