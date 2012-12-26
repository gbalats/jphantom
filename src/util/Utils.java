package util;

import java.util.*;

/** @author George Balatsouras */
public class Utils
{
    private Utils() {}

    public static <E> List<E> newList()
    {
        return new LinkedList<E>();
    }

    public static <E> List<E> newList(Collection<? extends E> c)
    {
        return new LinkedList<E>(c);
    }
    
    public static <E> Set<E> newSet()
    {
        return new HashSet<E>();
    }

    public static <E> Set<E> newSet(Collection<? extends E> c)
    {
        return new HashSet<E>(c);
    }

    public static <K,V> Map<K,V> newMap()
    {
        return new HashMap<K,V>();
    }

    public static <K,V> Map<K,V> newMap(Map<? extends K,? extends V> m)
    {
        return new HashMap<K,V>(m);
    }

    public static <E> Set<E> union(Set<? extends E> setA, Set<? extends E> setB) {
        Set<E> tmp = newSet(setA);
        tmp.addAll(setB);
        return tmp;
    }
}
