package personthecat.catlib.data.collections;

import java.util.IdentityHashMap;
import java.util.List;

public class MultiValueIdentityMap<K, V> extends IdentityHashMap<K, List<V>> implements MultiValueMap<K, V> {}
