package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.command.annotations.Nullable;
import personthecat.catlib.serialization.codec.CapturingCodec.Captor;
import personthecat.catlib.serialization.codec.CapturingCodec.Key;
import personthecat.catlib.serialization.codec.CapturingCodec.Receiver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CaptureCategory<O> {
    public static final Map<Key<?>, CaptureCategory<?>> CATEGORIES = new ConcurrentHashMap<>();
    private final Map<Key<?>, Prefix<?>> map = new HashMap<>();

    private CaptureCategory() {}

    @SafeVarargs
    public static <O> CaptureCategory<O> get(String name, O... implicitType) {
        return get(Key.of(name, implicitType));
    }

    public static <O> CaptureCategory<O> get(String name, Class<O> type) {
        return get(Key.of(name, type));
    }

    @SuppressWarnings("unchecked")
    private static <O> CaptureCategory<O> get(Key<O> key) {
        return (CaptureCategory<O>) CATEGORIES.computeIfAbsent(key, k -> new CaptureCategory<>());
    }

    @SafeVarargs
    public final <T> FieldDescriptor<O, T, T> field(Codec<T> type, String name, Function<O, T> getter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, null);
        return FieldDescriptor.defaultTry(type, name, prefix.receiver(), getter);
    }

    public final <T> FieldDescriptor<O, T, T> defaulted(Codec<T> type, String name, T def, Function<O, T> getter) {
        final var prefix = this.createPrefix(Key.of(name, def), type, def);
        return FieldDescriptor.defaultTry(type, name, prefix.receiver(), getter);
    }

    @SafeVarargs
    public final <T> DynamicField<O, O, T> field(Codec<T> type, String name, Function<O, T> getter, BiConsumer<O, T> setter, T... implicitType) {
        final var prefix = this.createPrefix(Key.of(name, implicitType), type, null);
        return DynamicField.field(type, name, getter, setter).withDefaultSupplier(prefix.receiver());
    }

    public final <T> DynamicField<O, O, T> defaulted(Codec<T> type, String name, T def, Function<O, T> getter, BiConsumer<O, T> setter) {
        final var prefix = this.createPrefix(Key.of(name, def), type, def);
        return DynamicField.field(type, name, getter, setter).withDefaultSupplier(prefix.receiver());
    }

    private <T> Prefix<T> createPrefix(Key<T> key, Codec<T> type, @Nullable T hardDefault) {
        final var prefix = new Prefix<>(key, type.optionalFieldOf(key.key()), hardDefault);
        this.map.put(key, prefix);
        return prefix;
    }

    public List<Captor<?>> captors() {
        return this.captors(p -> true);
    }

    public List<Captor<?>> captors(String... keys) {
        final var filter = Set.of(keys);
        return this.captors(p -> filter.contains(p.key.key()));
    }

    public List<Captor<?>> captors(Predicate<Prefix<?>> filter) {
        return this.map.values().stream().filter(filter).<Captor<?>>map(Prefix::captor).toList();
    }

    public record Prefix<T>(Key<T> key, MapCodec<Optional<T>> codec, @Nullable T hardDefault) {
        public Receiver<T> receiver() {
            return () -> CapturingCodec.get(this.key, this.hardDefault);
        }

        public Captor<T> captor() {
            return new CapturingCodec.DefaultGetter<>(this.key, this.codec);
        }
    }
}
