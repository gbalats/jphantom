package jphantom.hier;

/** @author George Balatsouras */
public class CyclicHierarchyException extends RuntimeException
{
    protected static final long serialVersionUID = 832664573456345L;

    public CyclicHierarchyException(String msg) {
        super(msg);
    }

    public CyclicHierarchyException() {
        super();
    }
}
