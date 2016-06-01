package util;

import java.util.*;

/** @author George Balatsouras */
public class Utils
{
    private Utils() {}

    public static <E> Set<E> union(Set<? extends E> setA, Set<? extends E> setB) {
        Set<E> tmp = new HashSet<>(setA);
        tmp.addAll(setB);
        return tmp;
    }
}
