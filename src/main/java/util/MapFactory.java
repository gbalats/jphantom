package util;

import java.util.*;

public class MapFactory<K,V> implements Factory<Map<K,V>>
{
    public MapFactory() {}

    @Override
    public Map<K,V> create() {
        return new HashMap<K,V>();
    }
}
