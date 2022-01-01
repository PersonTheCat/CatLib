package personthecat.catlib.serialization;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class DynamicField<B, R, T> {
    final @Nullable Codec<T> codec;
    final String key;
    final Function<R, T> getter;
    final BiConsumer<B, T> setter;
    final boolean up;

    public DynamicField(final @Nullable Codec<T> codec, final String key, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        this(codec, key, getter, setter, false);
    }

    public DynamicField(final @Nullable Codec<T> codec, final String key, final Function<R, T> getter, final BiConsumer<B, T> setter, final boolean up) {
        this.codec = codec;
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.up = up;
        if (codec == null && up) throw new IllegalArgumentException("Implicit fields cannot be recursive");
    }

    public static <B, R, T> DynamicField<B, R, T> field(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter);
    }

    public static <B, R, T> DynamicField<B, R, T> extend(final Codec<T> type, final String name, final Function<R, T> getter, final BiConsumer<B, T> setter) {
        return new DynamicField<>(type, name, getter, setter, true);
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

    public boolean isImplicit() {
        return this.up;
    }
}
