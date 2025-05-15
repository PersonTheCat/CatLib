package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class FieldDescriptor<O, T, R> {

    public final RecordCodecBuilder<O, T> f;

    public FieldDescriptor(final RecordCodecBuilder<O, T> f) {
        this.f = f;
    }

    public abstract R r(final T t);

    public static <O, T> FieldDescriptor<O, T, T> field(final Codec<T> type, final String name, final Function<O, T> getter) {
        return passthrough(type.fieldOf(name).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, Optional<T>, Optional<T>> optional(final Codec<T> type, final String name, final Function<O, Optional<T>> getter) {
        return passthrough(type.optionalFieldOf(name).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, T, T> defaulted(final Codec<T> type, final String name, final T def, final Function<O, T> getter) {
        return passthrough(type.optionalFieldOf(name, def).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, T, T> defaultGet(final Codec<T> type, final String name, final Supplier<T> def, final Function<O, T> getter) {
        return passthrough(type.optionalFieldOf(name).xmap(
            o -> o.orElseGet(def), t -> Objects.equals(t, def.get()) ? Optional.empty() : Optional.of(t)
        ).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, T, T> defaultTry(final Codec<T> type, final String name, final Supplier<DataResult<T>> def, final Function<O, T> getter) {
        return passthrough(type.optionalFieldOf(name).flatXmap(
            o -> o.map(DataResult::success).orElseGet(() -> def.get().mapError(e -> "No key " + name + "; " + e)),
            t -> DataResult.success(isDefaultResult(t, def.get()) ? Optional.empty() : Optional.of(t))
        ).forGetter(getter));
    }

    private static <T> boolean isDefaultResult(T actual, DataResult<T> def) {
        return def.mapOrElse(t -> Objects.equals(actual, t), e -> false);
    }

    public static <O, T> Passthrough<O, T> passthrough(final RecordCodecBuilder<O, T> f) {
        return new Passthrough<>(f);
    }

    public static <O, T> FieldDescriptor<O, Optional<T>, T> nullable(final Codec<T> type, final String name, final Function<O, T> getter) {
        return nullable(type.optionalFieldOf(name).forGetter(o -> Optional.ofNullable(getter.apply(o))));
    }

    public static <O, T> Nullable<O, T> nullable(final RecordCodecBuilder<O, Optional<T>> f) {
        return new Nullable<>(f);
    }

    public static <O, T> FieldDescriptor<O, T, T> defaultedUnion(final MapCodec<T> codec, final Supplier<T> def, final Function<O, T> getter) {
        return union(codec.orElseGet(def), getter);
    }

    public static <O, T> FieldDescriptor<O, T, T> union(final MapCodec<T> codec, final Function<O, T> getter) {
        return new Passthrough<>(RecordCodecBuilder.of(getter, codec));
    }

    public static class Passthrough<O, T> extends FieldDescriptor<O, T, T> {
        public Passthrough(final RecordCodecBuilder<O, T> f) {
            super(f);
        }

        @Override
        public T r(final T t) {
            return t;
        }
    }

    public static class Nullable<O, T> extends FieldDescriptor<O, Optional<T>, T> {
        public Nullable(final RecordCodecBuilder<O, Optional<T>> f) {
            super(f);
        }

        @Override
        public T r(final Optional<T> t) {
            return t.orElse(null);
        }
    }
}