package org.clyze.jphantom.access;

public abstract class ClassAccessEvent extends Event
{
    public final int access;

    private ClassAccessEvent(int access) {
        super(NOP);
        this.access = access;
    }

    public String toString() {
        return "Accessing class as: " + access + 
            " " + super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public static final ClassAccessEvent IS_ANNOTATION =
        new ClassAccessEvent(ACC_ABSTRACT | ACC_ANNOTATION | ACC_INTERFACE | ACC_PUBLIC) {};

    public static final ClassAccessEvent IS_INTERFACE =
        new ClassAccessEvent(ACC_ANNOTATION | ACC_INTERFACE | ACC_PUBLIC) {};
}
