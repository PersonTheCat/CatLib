package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.DelegatingOps;
import personthecat.catlib.mixin.DelegatingOpsAccessor;

public record TypedCodec<A>(Codec<A> wrapped, Class<A> type) implements Codec<A> {

    public static <A> TypedCodec<A> of(Codec<A> codec, Class<A> type) {
        return new TypedCodec<>(codec, type);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        if (this.type.isInstance(input)) {
            return DataResult.success(Pair.of(this.type.cast(input), input));
        }
        return this.wrapped.decode(ops, input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        if (isLiteralOps(ops)) {
            return DataResult.success((T) input);
        }
        return this.wrapped.encode(input, ops, prefix);
    }

    private static boolean isLiteralOps(DynamicOps<?> ops) {
        while (true) {
            if (ops instanceof LiteralOps) {
                return true;
            } else if (ops instanceof DelegatingOps<?>) {
                ops = ((DelegatingOpsAccessor<?>) ops).getDelegate();
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return "Typed<" + this.type.getSimpleName() + ">[" + this.wrapped + "]";
    }
}
