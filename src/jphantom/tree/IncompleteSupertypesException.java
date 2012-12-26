package jphantom.tree;

import java.util.*;
import org.objectweb.asm.Type;
import static util.Utils.*;

public class IncompleteSupertypesException extends Exception
{
    protected static final long serialVersionUID = 7834563458345L;

    private List<Type> supertypes = newList();

    public IncompleteSupertypesException(Collection<? extends Type> supertypes) {
        this.supertypes.addAll(supertypes);
    }

    public IncompleteSupertypesException() {}

    public List<Type> getSupertypes() {
        return supertypes;
    }
}
