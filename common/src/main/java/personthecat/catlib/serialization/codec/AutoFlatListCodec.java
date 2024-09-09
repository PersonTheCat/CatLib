package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AutoFlatListCodec<A> implements Codec<List<A>> {

    private final Codec<A> elementCodec;

    public AutoFlatListCodec(final Codec<A> elementCodec) {
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<T> encode(final List<A> input, final DynamicOps<T> ops, final T prefix) {
        if (input.size() == 1) {
            return this.elementCodec.encode(input.getFirst(), ops, prefix);
        }
        final ListBuilder<T> builder = ops.listBuilder();
        for (final A a : input) {
            builder.add(this.elementCodec.encodeStart(ops, a));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<List<A>, T>> decode(final DynamicOps<T> ops, final T input) {
        final ImmutableList.Builder<A> read = ImmutableList.builder();
        final Stream.Builder<T> failed = Stream.builder();
        final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

        this.decodeList(input, ops, read, failed, result);

        final ImmutableList<A> elements = read.build();
        final T errors = ops.createList(failed.build());
        final Pair<List<A>, T> pair = Pair.of(elements, errors);
        return result.getValue().map(unit -> pair).setPartial(pair);
    }

    private <T> void decodeList(final T input, final DynamicOps<T> ops, final ImmutableList.Builder<A> read,
                                final Stream.Builder<T> failed, final MutableObject<DataResult<Unit>> result) {
        // Recurse until not a list.
        final Optional<Consumer<Consumer<T>>> listResult = ops.getList(input).result();
        if (listResult.isPresent()) {
            listResult.get().accept(t -> this.decodeList(t, ops, read, failed, result));
        } else {
            final DataResult<Pair<A, T>> element = this.elementCodec.decode(ops, input);
            element.error().ifPresent(e -> failed.add(input));
            result.setValue(result.getValue().apply2stable((r, v) -> {
                read.add(v.getFirst());
                return r;
            }, element));
        }
    }

    @Override
    public String toString() {
        return "AutoFlatListCodec[" + this.elementCodec + "]";
    }
}
