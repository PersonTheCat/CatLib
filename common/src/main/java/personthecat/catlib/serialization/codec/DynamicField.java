package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class DynamicField<B, R, T> {
    final @Nullable Codec<T> codec;
    final @Nullable BiPredicate<R, T> outputFilter;
    final String key;
    final Function<R, T> getter;
    final BiConsumer<B, T> setter;
    final Type type;

    public DynamicField(
            final @Nullable Codec<T> codec,
            final String key,
            final Function<R, T> getter,
            final BiConsumer<B, T> setter) {
        this(codec, key, getter, setter, Type.IGNORE_NULL);
    }

    public DynamicField(
            final @Nullable Codec<T> codec,
            final String key,
            final Function<R, T> getter,
            final BiConsumer<B, T> setter,
            final Type type) {
        this(codec, key, getter, setter, type, null);
    }

    public DynamicField(
            final @Nullable Codec<T> codec,
            final String key,
            final Function<R, T> getter,
            final BiConsumer<B, T> setter,
            final Type type,
            final @Nullable BiPredicate<R, T> outputFilter) {
        this.codec = codec;
        this.outputFilter = outputFilter;
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
        if (codec == null && type == Type.IMPLICIT) throw new IllegalArgumentException("Implicit fields cannot be recursive");
    }

    public static <B, R, T> DynamicField<B, R, T> field(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter);
    }

    public static <B, R, T> DynamicField<B, R, T> extend(final MapCodec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type.codec(), name, getter, setter, Type.IMPLICIT);
    }

    public static <B, R, T> DynamicField<B, R, T> nullable(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter, Type.NULLABLE);
    }

    public static <B, R, T> DynamicField<B, R, T> required(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter, Type.NONNULL);
    }

    public static <B, R, T> DynamicField<B, R, T> recursive(final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(null, name, getter, setter);
    }

    public @Nullable Codec<T> type() {
        return this.codec;
    }

    public String key() {
        return this.key;
    }

    public Function<R, T> getter() {
        return this.getter;
    }

    public BiConsumer<B, T> setter() {
        return this.setter;
    }

    public @Nullable BiPredicate<R, T> getOutputFilter() {
        return this.outputFilter;
    }

    public DynamicField<B, R, T> withOutputFilter(final Predicate<T> filter) {
        return this.withOutputFilter((r, t) -> filter.test(t));
    }

    public DynamicField<B, R, T> withOutputFilter(final BiPredicate<R, T> filter) {
        return new DynamicField<>(this.codec, this.key, this.getter, this.setter, this.type, filter);
    }

    public DynamicField<B, R, T> ignoring(final R reader) {
        return this.withOutputFilter(t -> !Objects.equals(t, this.getter.apply(reader)));
    }

    public DynamicField<B, R, T> ignoringValue(final T value) {
        return this.withOutputFilter(t -> !Objects.equals(t, value));
    }

    public boolean isImplicit() {
        return this.type == Type.IMPLICIT;
    }

    public boolean isNullable() {
        return this.type == Type.NULLABLE;
    }

    public boolean isRequired() {
        return this.type == Type.NONNULL;
    }

    public enum Type {
        NONNULL,
        NULLABLE,
        IGNORE_NULL,
        IMPLICIT
    }
}
