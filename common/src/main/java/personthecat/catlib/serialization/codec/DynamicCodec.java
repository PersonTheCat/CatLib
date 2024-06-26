package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.apache.commons.lang3.mutable.MutableObject;
import personthecat.catlib.exception.UnreachableException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DynamicCodec<B, R, A> implements Codec<A> {
    private final Supplier<B> builder;
    private final Function<A, R> in;
    private final Function<B, A> out;
    private final Map<String, DynamicField<B, R, ?>> fields;
    private final Map<String, DynamicField<B, R, ?>> implicitFields;
    private final Map<String, DynamicField<B, R, ?>> requiredFields;

    @SafeVarargs
    public DynamicCodec(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out, final DynamicField<B, R, ?>... fields) {
        this(builder, in, out, createMap(fields));
    }

    protected DynamicCodec(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out, final Map<String, DynamicField<B, R, ?>> fields) {
        this.builder = builder;
        this.in = in;
        this.out = out;
        this.fields = fields;
        this.implicitFields = getFields(fields.values(), DynamicField.Type.IMPLICIT);
        this.requiredFields = getFields(fields.values(), DynamicField.Type.NONNULL);
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

    private static <B, R> Map<String, DynamicField<B, R, ?>> getFields(
        final Collection<DynamicField<B, R, ?>> all, final DynamicField.Type type) {

        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> fields = ImmutableMap.builder();
        for (final DynamicField<B, R, ?> field : all) {
            if (field.type == type) {
                fields.put(field.key, field);
            }
        }
        return fields.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        if (input == null) {
            return DataResult.error(() -> "Input is null");
        }
        final R reader = this.in.apply(input);
        final Map<T, T> map = new HashMap<>();
        final List<T> errors = new ArrayList<>();

        for (final DynamicField<B, R, ?> field : this.fields.values()) {
            final Object value = field.getter.apply(reader);
            if (value == null) {
                continue;
            }
            final Predicate<Object> filter = (Predicate<Object>) field.outputFilter;
            if (filter != null && !filter.test(value)) {
                continue;
            }
            Codec<Object> type = (Codec<Object>) field.codec;
            if (type == null) type = (Codec<Object>) this;
            if (field.isImplicit()) {
                type.encode(value, ops, prefix)
                    .resultOrPartial(e -> errors.add(ops.createString(e)))
                    .flatMap(t -> ops.getMapValues(t)
                        .resultOrPartial(e -> errors.add(ops.createString("Implicit value must be a map"))))
                    .ifPresent(values -> values
                        .forEach(pair -> map.put(pair.getFirst(), pair.getSecond())));
            } else {
                type.encodeStart(ops, value)
                    .resultOrPartial(e -> errors.add(ops.createString(e)))
                    .ifPresent(t -> map.put(ops.createString(field.key), t));
            }
        }
        if (!errors.isEmpty()) {
            return DataResult.error(() -> "Error encoding builder", ops.createList(errors.stream()));
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
            final Map<String, DynamicField<B, R, ?>> required = new HashMap<>(this.requiredFields);

            for (final DynamicField<B, R, ?> field : this.implicitFields.values()) {
                if (field.codec == null) throw new UnreachableException();
                final DataResult<Pair<Object, T>> element = ((Codec<Object>) field.codec).decode(ops, input);
                element.error().ifPresent(e -> failed.add(ops.createString(field.key)));
                result.setValue(result.getValue().apply2stable((r, v) -> {
                    ((BiConsumer<B, Object>) field.setter).accept(builder, v.getFirst());
                    return r;
                }, element));
            }
            for (final Map.Entry<String, DynamicField<B, R, ?>> entry : this.fields.entrySet()) {
                final DynamicField<B, R, Object> field = (DynamicField<B, R, Object>) entry.getValue();
                final T value = map.get(entry.getKey());
                if (field != null && !field.isImplicit()) {
                    if (value == null) {
                        if (field.isNullable()) {
                            field.setter.accept(builder, null);
                        }
                        continue;
                    }
                    required.remove(field.key);
                    Codec<Object> codec = field.codec;
                    if (codec == null) codec = (Codec<Object>) this;
                    final DataResult<Pair<Object, T>> element = codec.decode(ops,  value);

                    element.error().ifPresent(e -> failed.add(value));
                    result.setValue(result.getValue().apply2stable((r, v) -> {
                        field.setter.accept(builder, v.getFirst());
                        return r;
                    }, element));
                }
            }
            if (!required.isEmpty()) {
                for (final String missing : required.keySet()) {
                    failed.add(ops.createString(missing));
                }
                result.setValue(DataResult.error(() -> "Required values are missing"));
            }
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

        @SuppressWarnings("unchecked")
        public final DynamicCodec<B, R, A> create(final List<DynamicField<B, R, ?>> fields) {
            return this.create(fields.toArray(DynamicField[]::new));
        }

        @SafeVarargs
        public final DynamicCodec<B, R, A> create(final DynamicField<B, R, ?>... fields) {
            return new DynamicCodec<>(this.builder, this.in, this.out, fields);
        }
    }
}