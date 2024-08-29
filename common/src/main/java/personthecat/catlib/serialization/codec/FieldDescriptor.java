package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import personthecat.catlib.mixin.RecordCodecBuilderAccessor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class FieldDescriptor<O, T, R> {

    public final RecordCodecBuilder<O, T> f;

    public FieldDescriptor(final RecordCodecBuilder<O, T> f) {
        this.f = f;
    }

    public abstract R r(final T t);

    public T getReturn(final O o) {
        return this.accessor().getter().apply(o);
    }

    public MapEncoder<T> getEncoder(final O o) {
        return this.accessor().encoder().apply(o);
    }

    public MapDecoder<T> getDecoder() {
        return this.accessor().decoder();
    }

    @SuppressWarnings("unchecked")
    protected RecordCodecBuilderAccessor<O, T> accessor() {
        return (RecordCodecBuilderAccessor<O, T>) (Object) this.f;
    }

    public static <O, T> FieldDescriptor<O, T, T> field(final Codec<T> type, final String name, final Function<O, T> getter) {
        return passthrough(type.fieldOf(name).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, Optional<T>, T> nullable(final Codec<T> type, final String name, final Function<O, T> getter) {
        return new Nullable<>(new StrictOptionalCodec<>(type, name).wrapGetter(getter));
    }

    public static <O, T> Nullable<O, T> nullable(final RecordCodecBuilder<O, Optional<T>> f) {
        return new Nullable<>(f);
    }

    public static <O, T> FieldDescriptor<O, Optional<T>, Optional<T>> optional(final Codec<T> type, final String name, final Function<O, Optional<T>> getter) {
        return new Passthrough<>(new StrictOptionalCodec<>(type, name).forGetter(getter));
    }

    public static <O, T> FieldDescriptor<O, T, T> defaulted(final Codec<T> type, final String name, final T def, final Function<O, T> getter) {
        return defaultGet(type, name, () -> def, getter);
    }

    public static <O, T> FieldDescriptor<O, T, T> defaultGet(final Codec<T> type, final String name, final Supplier<T> def, final Function<O, T> getter) {
        return new Passthrough<>(new DefaultedMapCodec<>(type, name, def).forGetter(getter));
    }

    public static <O, T> Passthrough<O, T> passthrough(final RecordCodecBuilder<O, T> f) {
        return new Passthrough<>(f);
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