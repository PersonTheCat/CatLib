package personthecat.catlib.serialization.codec.capture;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import personthecat.catlib.command.annotations.Nullable;
import personthecat.catlib.serialization.codec.DynamicField;
import personthecat.catlib.serialization.codec.FieldDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CaptureCategory<O> {
    public static final Map<Key<?>, CaptureCategory<?>> CATEGORIES = new ConcurrentHashMap<>();
    private final KeyMap<Prefix<O, ?>> map = new KeyMap<>();
    private final Key<O> key;

    private CaptureCategory(Key<O> key) {
        this.key = key;
    }

    @SafeVarargs
    public static <O> CaptureCategory<O> get(String name, O... implicitType) {
        return get(Key.of(name, implicitType));
    }

    public static <O> CaptureCategory<O> get(String name, Class<O> type) {
        return get(Key.of(name, type));
    }

    @SuppressWarnings("unchecked")
    private static <O> CaptureCategory<O> get(Key<O> key) {
        return (CaptureCategory<O>) CATEGORIES.computeIfAbsent(key, CaptureCategory::new);
    }

    @SafeVarargs
    public final <T> FieldDescriptor<O, T, T> field(Codec<T> type, String name, Function<O, T> getter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, null);
        return FieldDescriptor.defaultTry(prefix.codec(), name, prefix.receiver(), getter);
    }

    @SafeVarargs
    public final <T> FieldDescriptor<O, T, T> defaulted(Codec<T> type, String name, T def, Function<O, T> getter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, def);
        return FieldDescriptor.defaultTry(prefix.codec(), name, prefix.receiver(), getter);
    }

    @SafeVarargs
    public final <T> FieldDescriptor<O, Optional<T>, T> nullable(Codec<T> type, String name, Function<O, T> getter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, null);
        return FieldDescriptor.nullableTry(prefix.codec(), name, prefix.optionalReceiver(), getter);
    }

    @SafeVarargs
    public final <T> DynamicField<O, O, T> field(Codec<T> type, String name, Function<O, T> getter, BiConsumer<O, T> setter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, null);
        return DynamicField.field(prefix.codec(), name, getter, setter).withDefaultSupplier(prefix.receiver());
    }

    @SafeVarargs
    public final <T> DynamicField<O, O, T> defaulted(Codec<T> type, String name, T def, Function<O, T> getter, BiConsumer<O, T> setter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, def);
        return DynamicField.field(prefix.codec(), name, getter, setter).withDefaultSupplier(prefix.receiver());
    }

    @SafeVarargs
    public final <T> DynamicField<O, O, T> nullable(Codec<T> type, String name, Function<O, T> getter, BiConsumer<O, T> setter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, getter, null);
        return DynamicField.nullable(prefix.codec(), name, getter, setter).withDefaultSupplier(prefix.nullableReceiver());
    }

    @SafeVarargs
    public final <T> Codec<T> configure(Codec<T> codec, T... implicitType) {
        return this.configure(codec, Function.identity(), "type", implicitType);
    }

    @SafeVarargs
    public final <T, R> Codec<R> configure(Codec<T> codec, Function<Codec<T>, Codec<R>> mapper, T... implicitType) {
        return this.configure(codec, mapper, "type", implicitType);
    }

    @SafeVarargs
    public final <T> Codec<T> configure(Codec<T> codec, String typeKey, T... implicitType) {
        return this.configure(codec, Function.identity(), typeKey, implicitType);
    }

    @SafeVarargs
    public final <T, R> Codec<R> configure(
            Codec<T> codec, Function<Codec<T>, Codec<R>> mapper, String typeKey, T... implicitType) {
        return new ConfiguredCodec<>(codec, Key.inferType(implicitType), typeKey, mapper);
    }

    private <T> Prefix<O, T> createPrefix(Key<T> key, Codec<T> type, Function<O, T> getter, @Nullable T hardDefault) {
        final var prefix = new Prefix<>(key = key.qualified(this.key), this.buildCodec(key, type), getter, hardDefault);
        this.map.put(key.unqualified(), prefix);
        return prefix;
    }

    private <T> Codec<T> buildCodec(Key<T> key, Codec<T> type) {
        return type instanceof ConfiguredCodec<?, T> c ? c.createReceiver(key) : type;
    }

    public <T> Captor<T> supply(String key, T def) {
        return this.getPrefix(Key.of(key, def)).supply(def);
    }

    @SuppressWarnings("unchecked")
    private <T> Prefix<O, T> getPrefix(Key<T> key) {
        return (Prefix<O, T>) Objects.requireNonNull(this.map.get(key), "No prefix with key " + key);
    }

    public List<Captor<?>> createPreset(O defaults) {
        return this.map.values().stream().<Captor<?>>map(p -> p.supplyFrom(defaults)).toList();
    }

    public List<Captor<?>> captors() {
        return this.captors(p -> true);
    }

    public List<Captor<?>> captors(String... keys) {
        final var filter = Set.of(keys);
        return this.captors(p -> filter.contains(p.key.name()));
    }

    public List<Captor<?>> captors(Predicate<Prefix<O, ?>> filter) {
        return this.map.values().stream().distinct().filter(filter).<Captor<?>>map(Prefix::captor).toList();
    }

    public record Prefix<O, T>(Key<T> key, Codec<T> codec, Function<O, T> getter, @Nullable T hardDefault) {
        public Receiver<T> receiver() {
            return () -> CapturingCodec.get(this.key, this.hardDefault);
        }

        public Receiver.OptionalReceiver<T> optionalReceiver() {
            return () -> CapturingCodec.getOptional(this.key, this.hardDefault);
        }

        public Receiver<T> nullableReceiver() {
            return this.optionalReceiver().nullable();
        }

        public Captor<T> captor() {
            return new DefaultGetter<>(this.key, this.codec.optionalFieldOf(this.key.name()));
        }

        public Captor<T> supplyFrom(O defaults) {
            return this.supply(this.getter.apply(defaults));
        }

        public Captor<T> supply(T hardDefault) {
            return new DefaultSupplier<>(this.key, hardDefault);
        }
    }

    private record ConfiguredCodec<A, B>(
            Codec<A> codec, Class<A> type, String typeKey, Function<Codec<A>, Codec<B>> mapper) implements Codec<B> {

        private Codec<B> createReceiver(Key<B> key) {
            return this.mapper.apply(CapturingCodec.receiveType(this.typeKey, key.as(this.type), this.codec));
        }

        @Override
        public <T> DataResult<Pair<B, T>> decode(DynamicOps<T> ops, T input) {
            return this.mapper.apply(this.codec).decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(B input, DynamicOps<T> ops, T prefix) {
            return this.mapper.apply(this.codec).encode(input, ops, prefix);
        }
    }
}
