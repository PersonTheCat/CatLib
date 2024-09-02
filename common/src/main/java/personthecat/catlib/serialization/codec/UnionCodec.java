package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

public class UnionCodec<L, R, A> extends MapCodec<A> {
    private final MapCodec<L> l;
    private final MapCodec<R> r;
    private final Function<A, DataResult<Pair<L, R>>> splitter;
    private final Function<Pair<L, R>, DataResult<A>> combiner;
    private final ErrorReducer<L, R, A> errorReducer;

    private UnionCodec(
            final MapCodec<L> l,
            final MapCodec<R> r,
            final Function<A, DataResult<Pair<L, R>>> splitter,
            final Function<Pair<L, R>, DataResult<A>> combiner) {
        this(l, r, splitter, combiner, ErrorReducer.defaultReducer());
    }

    private UnionCodec(
            final MapCodec<L> l,
            final MapCodec<R> r,
            final Function<A, DataResult<Pair<L, R>>> splitter,
            final Function<Pair<L, R>, DataResult<A>> combiner,
            final ErrorReducer<L, R, A> errorReducer) {
        this.l = l;
        this.r = r;
        this.splitter = splitter;
        this.combiner = combiner;
        this.errorReducer = errorReducer;
    }

    public static <L, R> Builder<L,  R> builder(final MapCodec<L> l, final MapCodec<R> r) {
        return new Builder<>(l, r);
    }

    public UnionCodec<L, R, A> reduceError(
            final Function<? super DataResult.Error<L>, ? extends DataResult<A>> left,
            final Function<? super DataResult.Error<R>, ? extends DataResult<A>> right) {
        return new UnionCodec<>(this.l, this.r, this.splitter, this.combiner, either -> either.map(left, right));
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return Stream.concat(this.l.keys(ops), this.r.keys(ops));
    }

    @Override
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final var lResult = this.l.decode(ops, input);
        if (lResult.isError()) return this.errorReducer.apply(Either.left(lResult.error().orElseThrow()));
        final var rResult = this.r.decode(ops, input);
        if (rResult.isError()) return this.errorReducer.apply(Either.right(rResult.error().orElseThrow()));
        return this.combiner.apply(Pair.of(lResult.getOrThrow(), rResult.getOrThrow()));
    }

    @Override
    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        return this.splitter.apply(input).mapOrElse(
            split -> this.r.encode(split.getSecond(), ops, this.l.encode(split.getFirst(), ops, prefix)),
            prefix::withErrorsFrom);
    }

    public record Builder<L, R>(MapCodec<L> l, MapCodec<R> r) {
        public <A> UnionCodec<L, R, A> create(
                final Function<A, DataResult<Pair<L, R>>> splitter, final Function<Pair<L, R>, DataResult<A>> combiner) {
            return new UnionCodec<>(this.l, this.r, splitter, combiner);
        }
    }

    private interface ErrorReducer<L, R, A> extends Function<Either<DataResult.Error<L>, DataResult.Error<R>>, DataResult<A>> {
        static <L, R, A> ErrorReducer<L, R, A> defaultReducer() {
            return either -> DataResult.error(either.map(Function.identity(), Function.identity()).messageSupplier());
        }
    }
}
