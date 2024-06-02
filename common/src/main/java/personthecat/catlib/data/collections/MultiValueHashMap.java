package personthecat.catlib.data.collections;

import java.util.HashMap;
import java.util.List;

public class MultiValueHashMap<K, V> extends HashMap<K, List<V>> implements MultiValueMap<K, V> {}
