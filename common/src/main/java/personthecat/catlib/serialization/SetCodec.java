package personthecat.catlib.serialization;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Set;
import java.util.stream.Stream;

public class SetCodec<A> implements Codec<Set<A>> {

    private final Codec<A> elementCodec;

    public SetCodec(final Codec<A> elementCodec) {
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<T> encode(final Set<A> input, final DynamicOps<T> ops, final T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();

        for (final A a : input) {
            builder.add(elementCodec.encodeStart(ops, a));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<Set<A>, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
            final ImmutableSet.Builder<A> read = ImmutableSet.builder();
            final Stream.Builder<T> failed = Stream.builder();
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

            stream.accept(t -> {
                final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t);
                element.error().ifPresent(e -> failed.add(t));
                result.setValue(result.getValue().apply2stable((r, v) -> {
                    read.add(v.getFirst());
                    return r;
                }, element));
            });

            final ImmutableSet<A> elements = read.build();
            final T errors = ops.createList(failed.build());

            final Pair<Set<A>, T> pair = Pair.of(elements, errors);

            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }

    @Override
    public String toString() {
        return "SetCodec[" + this.elementCodec + "]";
    }
}
