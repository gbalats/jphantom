package util;

import java.util.*;

public class ForwardingMap<K,V> implements Map<K,V>
{
    protected final Map<K,V> m;

    public ForwardingMap(Map<K,V> base) {
        this.m = base;
    }

    @Override public void clear() { m.clear(); }
    @Override public int hashCode() { return m.hashCode(); }
    @Override public V get(Object key) { return m.get(key); }
    @Override public boolean isEmpty() { return m.isEmpty(); }
    @Override public Set<Map.Entry<K,V>> entrySet() { return m.entrySet(); }
    @Override public boolean equals(Object o) { return m.equals(o); }
    @Override public V put(K key, V value) { return m.put(key,value); }
    @Override public void putAll(Map<? extends K,? extends V> mm) {
        m.putAll(mm); 
    }
    @Override public int size() { return m.size(); }
    @Override public V remove(Object key) { return m.remove(key); }
    @Override public Collection<V> values() { return m.values(); }
    @Override public boolean containsKey(Object key) { 
        return m.containsKey(key); }
    @Override public boolean containsValue(Object value) { 
        return m.containsValue(value); }
    @Override public Set<K> keySet() { return m.keySet();}
}
