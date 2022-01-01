package personthecat.catlib.serialization;

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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DynamicCodec<B, R, A> implements Codec<A> {
    private final Supplier<B> builder;
    private final Function<A, R> in;
    private final Function<B, A> out;
    private final Map<String, DynamicField<B, R, ?>> fields;

    @SafeVarargs
    public DynamicCodec(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out, final DynamicField<B, R, ?>... fields) {
        this(builder, in, out, createMap(fields));
    }

    protected DynamicCodec(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out, final Map<String, DynamicField<B, R, ?>> fields) {
        this.builder = builder;
        this.in = in;
        this.out = out;
        this.fields = fields;
    }

    public static <B, R, A> Builder<B, R, A> builder(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out) {
        return new Builder<>(builder, in, out);
    }

    public DynamicCodec<B, R, A> withBuilder(final Supplier<B> builder) {
        return new DynamicCodec<>(builder, this.in, this.out, this.fields);
    }

    public DynamicCodec<B, R, A> withReader(final Function<A, R> reader) {
        return new DynamicCodec<>(this.builder, reader, this.out, this.fields);
    }

    public DynamicCodec<B, R, A> withWriter(final Function<B, A> writer) {
        return new DynamicCodec<>(this.builder, this.in, writer, this.fields);
    }

    @SafeVarargs
    public final DynamicCodec<B, R, A> withMoreFields(final DynamicField<B, R, ?>... fields) {
        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> map = ImmutableMap.builder();
        map.putAll(this.fields);
        map.putAll(createMap(fields));
        return new DynamicCodec<>(this.builder, this.in, this.out, map.build());
    }

    public DynamicCodec<B, R, A> withoutFields(final String... fields) {
        final Map<String, DynamicField<B, R, ?>> map = new HashMap<>(this.fields);
        for (final String field : fields) {
            map.remove(field);
        }
        return new DynamicCodec<>(this.builder, this.in, this.out, ImmutableMap.copyOf(map));
    }

    @SafeVarargs
    public final DynamicCodec<B, R, A> withoutFields(final DynamicField<B, R, ?>... fields) {
        final Map<String, DynamicField<B, R, ?>> map = new HashMap<>(this.fields);
        for (final DynamicField<B, R, ?> field : fields) {
            map.remove(field.key);
        }
        return new DynamicCodec<>(this.builder, this.in, this.out, ImmutableMap.copyOf(map));
    }

    private static <B, R> Map<String, DynamicField<B, R, ?>> createMap(final DynamicField<B, R, ?>[] fields) {
        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> map = ImmutableMap.builder();
        for (final DynamicField<B, R, ?> field : fields) {
            map.put(field.key, field);
        }
        return map.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        final R reader = this.in.apply(input);
        final Map<T, T> map = new HashMap<>();
        final List<T> errors = new ArrayList<>();

        for (final DynamicField<B, R, ?> field : this.fields.values()) {
            Codec<Object> type = (Codec<Object>) field.codec;
            if (type == null) type = (Codec<Object>) this;
            type.encodeStart(ops, field.getter.apply(reader))
                .resultOrPartial(e -> errors.add(ops.createString(e)))
                .ifPresent(t -> map.put(ops.createString(field.key), t));
        }
        if (!errors.isEmpty()) {
            return DataResult.error("Error encoding builder", ops.createList(errors.stream()));
        }
        return ops.mergeToMap(prefix, map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getMap(input).flatMap(map -> {
            final B builder = this.builder.get();
            final Stream.Builder<T> failed = Stream.builder();
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE));

            map.entries().forEach(pair -> {
                final DataResult<Pair<String, T>> key = Codec.STRING.decode(ops, pair.getFirst());

                key.resultOrPartial(e -> failed.add(pair.getFirst())).ifPresent(k -> {
                    final DynamicField<B, R, Object> field = (DynamicField<B, R, Object>) this.fields.get(k.getFirst());
                    if (field != null) {
                        Codec<Object> codec = field.codec;
                        if (codec == null) codec = (Codec<Object>) this;
                        final DataResult<Pair<Object, T>> element = codec.decode(ops,  pair.getSecond());

                        element.error().ifPresent(e -> failed.add(pair.getSecond()));
                        result.setValue(result.getValue().apply2stable((r, v) -> {
                            field.setter.accept(builder, v.getFirst());
                            return r;
                        }, element));
                    }
                });
            });

            final Pair<A, T> pair = Pair.of(this.out.apply(builder), ops.createList(failed.build()));
            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }

    public static class Builder<B, R, A> {
        private final Supplier<B> builder;
        private final Function<A, R> in;
        private final Function<B, A> out;

        public Builder(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out) {
            this.builder = builder;
            this.in = in;
            this.out = out;
        }

        @SafeVarargs
        public final DynamicCodec<B, R, A> create(final DynamicField<B, R, ?>... fields) {
            return new DynamicCodec<>(this.builder, this.in, this.out, fields);
        }
    }
}