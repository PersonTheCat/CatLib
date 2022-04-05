package personthecat.catlib.data.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class MultiValueHashMap<K, V> extends HashMap<K, List<V>> implements MultiValueMap<K, V> {

    @Override
    public void add(final K k, final V v) {
        if (!containsKey(k)) {
            put(k, new ArrayList<>());
        }
        get(k).add(v);
    }
}
