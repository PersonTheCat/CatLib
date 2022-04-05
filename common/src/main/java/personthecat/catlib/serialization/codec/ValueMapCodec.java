package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ValueMapCodec<A> implements Codec<Map<String, A>> {
    private final Codec<A> codec;

    public ValueMapCodec(final Codec<A> codec) {
        this.codec = codec;
    }

    public Codec<A> getType() {
        return this.codec;
    }

    @Override
    public <T> DataResult<T> encode(final Map<String, A> input, final DynamicOps<T> ops, final T prefix) {
        final Map<T, T> map = new HashMap<>();
        final List<T> errors = new ArrayList<>();

        for (final Map.Entry<String, A> entry : input.entrySet()) {
            this.codec.encodeStart(ops, entry.getValue())
                .resultOrPartial(e -> errors.add(ops.createString(e)))
                .ifPresent(t -> map.put(ops.createString(entry.getKey()), t));
        }
        if (!errors.isEmpty()) {
            return DataResult.error("Error encoding map", ops.createList(errors.stream()));
        }
        return ops.mergeToMap(prefix, map);
    }

    @Override
    public <T> DataResult<Pair<Map<String, A>, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getMap(input).flatMap(map -> {
            final ImmutableMap.Builder<String, A> out = ImmutableMap.builder();
            final Stream.Builder<T> failed = Stream.builder();
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE));

            map.entries().forEach(pair -> {
                final DataResult<Pair<String, T>> key = Codec.STRING.decode(ops, pair.getFirst());
                final DataResult<Pair<A, T>> element = this.codec.decode(ops, pair.getSecond());

                key.resultOrPartial(e -> failed.add(pair.getFirst())).ifPresent(k -> {
                    element.error().ifPresent(e -> failed.add(pair.getSecond()));
                    result.setValue(result.getValue().apply2stable((r, v) -> {
                        out.put(k.getFirst(), v.getFirst());
                        return r;
                    }, element));
                });
            });

            final Pair<Map<String, A>, T> pair = Pair.of(out.build(), ops.createList(failed.build()));
            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }

    @Override
    public String toString() {
        return "ValueMapCodec[String -> " + this.codec + "]";
    }
}
