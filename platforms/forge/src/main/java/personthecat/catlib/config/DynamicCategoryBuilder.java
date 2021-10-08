package personthecat.catlib.config;

import com.electronwill.nightconfig.core.Config;
import lombok.AllArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import personthecat.catlib.data.MultiValueHashMap;
import personthecat.catlib.data.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DynamicCategoryBuilder {

    public static Stub withPath(final String path) {
        return new Stub(path);
    }

    @AllArgsConstructor
    public static class Stub {
        private final String path;

        public final ToggleBuilder withBooleanEntries(final Map<String, Boolean> entries) {
            return new ToggleBuilder(this.path, entries);
        }

        public final ToggleBuilder withBooleanEntries(final Collection<String> entries) {
            return withBooleanEntries(entries.stream().collect(Collectors.toMap(k -> k, k -> true)));
        }

        public final ToggleBuilder withBoolean(final String entry, final boolean value) {
            return this.withBooleanEntries(new HashMap<>()).withBoolean(entry, value);
        }

        public final <T> ListBuilder<T> withListEntries(final MultiValueMap<String, T> entries) {
            return new ListBuilder<>(this.path, entries);
        }

        @SafeVarargs
        public final <T> ListBuilder<T> withList(final String entry, final T... values) {
            return this.<T>withListEntries(new MultiValueHashMap<>()).withList(entry, values);
        }

        public final <T> ListBuilder<T> withList(final String entry, final List<T> values) {
            return this.<T>withListEntries(new MultiValueHashMap<>()).withList(entry, values);
        }
    }

    public static class ToggleBuilder {
        private final String path;
        private final Map<String, Boolean> map;
        private boolean def;

        private ToggleBuilder(final String path, final Map<String, Boolean> map) {
            this.path = path;
            this.map = map;
            this.def = false;
        }

        public final ToggleBuilder withBoolean(final String entry, final boolean value) {
            this.map.put(entry, value);
            return this;
        }

        public ToggleBuilder withDefaultValue(final boolean def) {
            this.def = def;
            return this;
        }

        public DynamicCategory<Boolean> build(final ForgeConfigSpec.Builder builder, final Config cfg) {
            return new DynamicCategory<>(Boolean.class, cfg, builder, path, this.map,  this.def);
        }
    }

    public static class ListBuilder<T> {
        private final String path;
        private final MultiValueMap<String, T> map;
        private List<T> def;

        private ListBuilder(final String path, final MultiValueMap<String, T> map) {
            this.path = path;
            this.map = map;
            this.def = null;
        }

        @SafeVarargs
        public final ListBuilder<T> withList(final String entry, final T... values) {
            this.map.put(entry, asList(values));
            return this;
        }

        public final ListBuilder<T> withList(final String entry, final List<T> values) {
            this.map.put(entry, values);
            return this;
        }

        public ListBuilder<T> withDefaultValue(final List<T> def) {
            this.def = def;
            return this;
        }

        public ListBuilder<T> withDefaultValue(final T def) {
            this.def = Collections.singletonList(def);
            return this;
        }

        @SuppressWarnings("unchecked")
        public DynamicCategory<List<T>> build(final ForgeConfigSpec.Builder builder, final Config cfg) {
            return new DynamicCategory<>((Class<List<T>>) (Object) List.class, cfg, builder, this.path, this.map, null);
        }

        @SafeVarargs
        private static <T> List<T> asList(final T... values) {
            final List<T> list = new ArrayList<>();
            Collections.addAll(list, values);
            return list;
        }
    }
}
