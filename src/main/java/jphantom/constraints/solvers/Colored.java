package jphantom.constraints.solvers;

import jphantom.exc.*;

class Colored<V> {
    public enum Color { BLACK, WHITE, GREY };
    public enum Type { CLASS, INTERFACE };

    private final V vertex;
    private Type type;
    protected Color color;

    Colored(V vertex) {
        this.vertex = vertex;
        this.type = null;
    }

    public void setType(Type type)
    {
        if (type == null)
            throw new IllegalArgumentException("null type");

        if (resolved() && type != this.type)
            throw new ConflictingTypeException();
            
        this.type = type;
    }

    public Type getType() {
        if (type == null)
            throw new IllegalStateException("unresolved");
        return type;
    }

    public boolean resolved() {
        return type != null;
    }

    public V get() {
        return vertex;
    }

    @Override
    public int hashCode() { return vertex.hashCode(); }
        
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Colored))
            return false;

        Colored<?> other = (Colored<?>) obj;

        return vertex.equals(other.vertex);
    }
}
