package personthecat.catlib.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class ConfigUtilImpl {
    private ConfigUtilImpl() {}

    public static Object remap(Class<?> t, Class<?>[] generics, Object o) {
        return remap(t, generics, 0, o);
    }

    private static Object remap(Class<?> t, Class<?>[] generics, int idx, Object o) {
        if (idx > generics.length) {
            throw new IllegalStateException("Generic types on config field did not resolve correctly");
        }
        if (o instanceof ForgeConfigSpec.ConfigValue<?> v) {
            o = v.get();
        }
        if (o instanceof com.electronwill.nightconfig.core.Config c) {
            final Map map = new HashMap<>();
            for (final Map.Entry e : c.valueMap().entrySet()) {
                map.put(e.getKey(), remap(generics[idx], generics, idx + 1, e.getValue()));
            }
            return map;
        } else if (Set.class.isAssignableFrom(t) && o instanceof List l) {
            final Set<Object> set = new HashSet<>();
            for (final Object e : l) {
                set.add(remap(generics[idx], generics, idx + 1, e));
            }
            return set;
        } else if (MultiValueMap.class.isAssignableFrom(t) && o instanceof Map m) {
            final MultiValueMap<Object, Object> mvm = new MultiValueHashMap<>();
            for (final Map.Entry e : (Set<Map.Entry>) m.entrySet()) {
                final Object v = remap(generics[idx], generics, idx + 1, e.getValue());
                final List<Object> l;
                if (v instanceof List) {
                    l = (List) v;
                } else {
                    l = new ArrayList<>();
                    l.add(v);
                }
                mvm.put(e.getKey(), l);
            }
            return mvm;
        } else if (t.isArray() && o instanceof List l) {
            final Object array = Array.newInstance(t.componentType(), l.size());
            for (int i = 0; i < l.size(); i++) {
                Array.set(array, i, remap(generics[idx], generics, idx + 1, l.get(i)));
            }
            return array;
        }
        return ConfigUtil.toCorrectPrimitive(t, o);
    }
}
