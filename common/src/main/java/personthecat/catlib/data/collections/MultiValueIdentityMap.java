package personthecat.catlib.data.collections;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@SuppressWarnings("unused")
public class MultiValueIdentityMap<K, V> extends IdentityHashMap<K, List<V>> implements MultiValueMap<K, V> {

    @Override
    public void add(final K k, final V v) {
        if (!containsKey(k)) {
            put(k, new ArrayList<>());
        }
        get(k).add(v);
    }
}
