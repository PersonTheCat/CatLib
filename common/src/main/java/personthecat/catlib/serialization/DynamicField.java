package personthecat.catlib.serialization;

import com.mojang.serialization.Codec;

import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class DynamicField<B, R, T> {
    final Codec<T> codec;
    final String key;
    final Function<R, T> getter;
    final BiConsumer<B, T> setter;

    public DynamicField(final Codec<T> codec, final String key, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        this.codec = codec;
        this.key = key;
        this.getter = getter;
        this.setter = setter;
    }

    public static <B, R, T> DynamicField<B, R, T> field(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter);
    }

    public Codec<T> type() {
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
}