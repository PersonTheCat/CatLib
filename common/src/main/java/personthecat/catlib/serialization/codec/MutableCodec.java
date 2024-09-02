package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class MutableCodec<A> implements Codec<A> {
    private Codec<A> current;
    private Codec<A> backup;

    private MutableCodec(final Codec<A> codec) {
        this.current = codec;
        this.backup = null;
    }

    public static <A> MutableCodec<A> wrap(final Codec<A> codec) {
        return codec instanceof MutableCodec<A> mutable ? mutable : new MutableCodec<>(codec);
    }

    public Codec<A> get() {
        return this.current;
    }

    public void set(final Codec<A> codec) {
        if (this.backup != null) this.backup = this.current;
        this.current = codec;
    }

    public void restore() {
        if (this.backup != null) {
            this.current = this.backup;
            this.backup = null;
        }
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        return this.current.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        return this.current.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "Mutable " + this.current;
    }
}
