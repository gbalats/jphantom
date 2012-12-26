package jphantom.access;

import java.lang.annotation.ElementType;

public interface ApplicableToElements {
    /**
     * Returns {@code true} iff this object applies to the specified element.
     *
     * @param type an element type
     * @return {@code true} if this object applies to the specified element; 
     *         {@code false} otherwise
     * @see java.lang.annotation.ElementType
     */
    public boolean appliesTo(ElementType type);
}
