package personthecat.catlib.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.data.IdMatcher.Info;
import personthecat.catlib.data.IdMatcher.InvertibleEntry;
import personthecat.catlib.data.IdMatcher.StringRepresentable;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.codec.DynamicField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.codec.DynamicField.field;
import static personthecat.catlib.serialization.codec.CodecUtils.dynamic;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;

public class IdList<T> implements Predicate<Holder<T>> {
    protected final RegistryHandle<T> handle;
    protected final List<InvertibleEntry> entries;
    protected final Format format;
    protected final boolean blacklist;
    protected HolderSet<T> compiled;
    protected Predicate<Holder<T>> optimized;

    protected IdList(
            final ResourceKey<? extends Registry<T>> key,
            final List<InvertibleEntry> entries,
            final boolean blacklist,
            final Format format) {
        this.handle = DynamicRegistries.get(key);
        this.entries = entries;
        this.format = format;
        this.blacklist = blacklist;
        DynamicRegistries.listen(this.handle, this)
            .accept(this::onRegistryUpdated);
    }

    @Override
    public boolean test(final Holder<T> holder) {
        return this.optimize().test(holder);
    }

    public Predicate<Holder<T>> optimize() {
        if (this.optimized != null) {
            return this.optimized;
        }
        if (this.isEmpty()) {
            return this.blacklist ? holder -> true : holder -> false;
        }
        synchronized (this) {
            final RegistryHandle<T> handle = this.handle;
            final HolderSet<T> compiled = this.compile();
            if (compiled.size() < (handle.size() / 2)) {
                return this.optimized = compiled::contains;
            }
            final HolderSet<T> inverted = invert(handle, compiled);
            return this.optimized = Predicate.not(inverted::contains);
        }
    }

    public synchronized HolderSet<T> compile() {
        if (this.compiled != null) {
            return this.compiled;
        }
        if (this.isEmpty()) {
            return HolderSet.direct();
        }
        final Set<Holder<T>> matching = new HashSet<>();
        for (final ResourceLocation id : this.compileIds()) {
            final Holder<T> holder = this.handle.getHolder(id);
            if (holder != null) {
                matching.add(holder);
            }
        }
        return this.compiled = toHolderSet(matching);
    }

    public Set<ResourceLocation> compileIds() {
        Set<ResourceLocation> white = new HashSet<>();
        Set<ResourceLocation> black = new HashSet<>();
        for (final InvertibleEntry e : this.entries) {
            e.add(this.handle, white, black);
        }
        if (this.blacklist) {
            final Set<ResourceLocation> temp = white;
            white = black;
            black = temp;
        }
        if (!white.isEmpty()) {
            white.removeAll(black);
            return white;
        }
        white.addAll(this.handle.keySet());
        white.removeAll(black);
        return white;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    protected void onRegistryUpdated(final RegistryHandle<T> actual) {
        this.compiled = null;
        this.optimized = null;
    }

    @SuppressWarnings("unchecked")
    protected static <T> HolderSet<T> toHolderSet(final Set<Holder<T>> set) {
        return HolderSet.direct(set.toArray(Holder[]::new));
    }

    protected static <T> HolderSet<T> invert(final RegistryHandle<T> handle, final HolderSet<T> set) {
        final Set<Holder<T>> inverted = new HashSet<>();
        handle.forEachHolder((id, holder) -> inverted.add(holder));
        set.forEach(inverted::remove);
        return toHolderSet(inverted);
    }

    public static <T> Builder<T> builder(final ResourceKey<? extends Registry<T>> key) {
        return new Builder<>(key);
    }

    public static <T> IdList<T> all(final ResourceKey<? extends Registry<T>> key) {
        return new IdList<>(key, List.of(), true, Format.OBJECT);
    }

    public static <T> Codec<IdList<T>> codecOf(final ResourceKey<? extends Registry<T>> key) {
        return codecOf(key, false);
    }

    public static <T> Codec<IdList<T>> codecOf(final ResourceKey<? extends Registry<T>> key, final boolean filter) {
        return codecFromTypes(key, IdMatcher.DEFAULT_TYPES, filter);
    }

    public static <T> Codec<IdList<T>> codecFromTypes(
            final ResourceKey<? extends Registry<T>> key, final List<Info<?>> types, final boolean filter) {
        return codecFromTypes(key, types, filter, (Constructor<T, IdList<T>>) IdList::new);
    }

    protected static <T, R extends IdList<T>> Codec<R> codecFromTypes(
            final ResourceKey<? extends Registry<T>> key,
            final List<Info<?>> types,
            final boolean filter,
            final Constructor<T, R> constructor) {
        final Codec<R> list = listCodec(key, types, filter, constructor);
        final Codec<R> all = allCodec(key, constructor);
        final Codec<R> object = objectCodec(key, types, constructor);
        return simpleAny(list, all, object)
            .withEncoder((R r) -> switch (r.getActualFormat()) {
                case LIST -> list;
                case OBJECT -> r.isExplicitAll() ? all : object;
                case ANY -> r.isExplicitAll() ? all : list;
            });
    }

    protected static <T, R extends IdList<T>> Codec<R> listCodec(
            final ResourceKey<? extends Registry<T>> key,
            final List<Info<?>> types,
            final boolean filter,
            final Constructor<T, R> constructor) {
        return easyList(entryCodec(types)).xmap(
            entries -> constructor.construct(key, entries, filter && entries.isEmpty(), Format.LIST),
            l -> l.entries);
    }

    protected static Codec<InvertibleEntry> entryCodec(final List<Info<?>> types) {
        final Map<Info<?>, Codec<InvertibleEntry>> encoderMap = new HashMap<>();
        final Map<String, Codec<InvertibleEntry>> decoderMap = new HashMap<>();
        for (final Info<?> info : types) {
            if (info instanceof StringRepresentable<?> r && r.prefix() != null) {
                final Codec<InvertibleEntry> codec = info.prefixedCodec();
                encoderMap.put(info, codec);
                decoderMap.put(r.prefix(), codec);
            }
        }
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(final InvertibleEntry input, final DynamicOps<T> ops, final T prefix) {
                final Info<?> info = input.matcher().info();
                final Codec<InvertibleEntry> encoder = encoderMap.get(info);
                if (encoder == null) {
                    return DataResult.error(() -> "no encoder for type: " + info);
                }
                return encoder.encode(input, ops, prefix);
            }

            @Override
            public <T> DataResult<Pair<InvertibleEntry, T>> decode(final DynamicOps<T> ops, final T input) {
                return ops.getStringValue(input).flatMap(s -> {
                    for (final Map.Entry<String, Codec<InvertibleEntry>> e : decoderMap.entrySet()) {
                        if (!e.getKey().isEmpty() && s.startsWith(e.getKey())) {
                            return e.getValue().decode(ops, input);
                        }
                    }
                    final Codec<InvertibleEntry> decoder = decoderMap.get("");
                    if (decoder == null) {
                        return DataResult.error(() -> "no default decoder");
                    }
                    return decoder.decode(ops, input);
                });
            }
        };
    }

    protected static <T, R extends IdList<T>> Codec<R> allCodec(
            final ResourceKey<? extends Registry<T>> key, final Constructor<T, R> constructor) {
        return Codec.BOOL.fieldOf("all").codec()
            .xmap(all -> constructor.construct(key, List.of(), all, Format.OBJECT), IdList::isExplicitAll);
    }

    protected static <T, R extends IdList<T>> Codec<R> objectCodec(
            final ResourceKey<? extends Registry<T>> key,
            final List<Info<?>> types,
            final Constructor<T, R> constructor) {
        final List<DynamicField<Builder<T>, R, ?>> fields = new ArrayList<>();
        for (final Info<?> i : types) {
            final DynamicField<Builder<T>, R, List<InvertibleEntry>> field =
                field(easyList(i.codec()), i.fieldName(), r -> r.getByType(i), Builder::addEntries);
            fields.add(field.withOutputFilter(list -> !list.isEmpty()));
        }
        final DynamicField<Builder<T>, R, Boolean> blacklistField =
            field(Codec.BOOL, "blacklist", r -> r.blacklist, Builder::blacklist);
        fields.add(blacklistField.withOutputFilter(blacklist -> blacklist));
        return dynamic(
                () -> new Builder<>(key),
                b -> b.format(Format.OBJECT).build(constructor))
            .create(fields);
    }

    protected List<InvertibleEntry> getByType(final Info<?> info) {
        return this.entries.stream().filter(e -> e.matcher().info() == info).toList();
    }

    public Format getActualFormat() {
        if (this.format == Format.OBJECT && !this.canBeList()) {
            return Format.OBJECT;
        }
        return this.format;
    }

    protected boolean canBeList() {
        return !this.blacklist && this.entries.stream().allMatch(e -> e.matcher().info().canBeListed());
    }

    public IdList<T> withBlacklist(final boolean blacklist) {
        return new IdList<>(this.handle.key(), this.entries, blacklist, this.format);
    }

    public boolean isExplicitAll() {
        return this.isEmpty() && this.blacklist;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.handle.key(), this.entries, this.blacklist, this.format);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IdList<?> l) {
            return this.handle.key().equals(l.handle.key())
                && this.entries.equals(l.entries)
                && this.blacklist == l.blacklist
                && this.format == l.format;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append("[key=").append(this.handle.key());
        sb.append(",size=").append(this.entries.size());
        if (this.blacklist) {
            sb.append(",blacklist=true");
        }
        if (this.format != Format.ANY) {
            sb.append(",format=").append(this.format);
        }
        return sb.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder<T> {
        protected final ResourceKey<? extends Registry<T>> key;
        protected final List<InvertibleEntry> entries = new ArrayList<>();
        protected Format format = Format.ANY;
        protected boolean blacklist = false;

        public Builder(final ResourceKey<? extends Registry<T>> key) {
            this.key = key;
        }

        public Builder<T> addEntries(final InvertibleEntry... entries) {
            return this.addEntries(List.of(entries));
        }

        public Builder<T> addEntries(final List<InvertibleEntry> entries) {
            this.entries.addAll(entries);
            return this;
        }

        public Builder<T> format(final Format format) {
            this.format = format;
            return this;
        }

        public Builder<T> blacklist(final boolean blacklist) {
            this.blacklist = blacklist;
            return this;
        }

        public IdList<T> build() {
            return this.build(IdList::new);
        }

        public <R extends IdList<T>> R build(final Constructor<T, R> constructor) {
            return constructor.construct(this.key, this.entries, this.blacklist, this.format);
        }
    }

    @FunctionalInterface
    public interface Constructor<T, R extends IdList<T>> {
        R construct(
            ResourceKey<? extends Registry<T>> key, List<InvertibleEntry> entries, boolean blacklist, Format format);
    }

    public enum Format {
        LIST,
        OBJECT,
        ANY
    }
}
