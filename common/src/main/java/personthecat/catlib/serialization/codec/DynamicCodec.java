package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import personthecat.catlib.exception.UnreachableException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DynamicCodec<B, R, A> extends MapCodec<A> {
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
            map.remove(field.key());
        }
        return new DynamicCodec<>(this.builder, this.in, this.out, ImmutableMap.copyOf(map));
    }

    public final DynamicCodec<B, R, A> ignoring(final R reader) {
        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> applied = ImmutableMap.builder();
        this.fields.forEach((key, field) -> applied.put(key, field.ignoring(reader)));
        return new DynamicCodec<>(this.builder, this.in, this.out, applied.build());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final DynamicCodec<B, R, A> applyFilters(final Function<String, BiPredicate<R, Object>> filterGetter) {
        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> applied = ImmutableMap.builder();
        this.fields.forEach((key, field) -> {
            final BiPredicate<R, ?> filter = filterGetter.apply(key);
            applied.put(key, filter != null ? field.withOutputFilter((BiPredicate) filter) : field);
        });
        return new DynamicCodec<>(this.builder, this.in, this.out, applied.build());
    }

    private static <B, R> Map<String, DynamicField<B, R, ?>> createMap(final DynamicField<B, R, ?>[] fields) {
        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> map = ImmutableMap.builder();
        for (final DynamicField<B, R, ?> field : fields) {
            map.put(field.key(), field);
        }
        return map.build();
    }

    private static <B, R> Map<String, DynamicField<B, R, ?>> getFields(
        final Collection<DynamicField<B, R, ?>> all, final DynamicField.Type type) {

        final ImmutableMap.Builder<String, DynamicField<B, R, ?>> fields = ImmutableMap.builder();
        for (final DynamicField<B, R, ?> field : all) {
            if (field.type() == type) {
                fields.put(field.key(), field);
            }
        }
        return fields.build();
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return this.fields.values().stream().map(f -> ops.createString(f.key()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final B builder = this.builder.get();
        final Map<String, Supplier<String>> errors = new HashMap<>();
        final Map<String, DynamicField<B, R, ?>> required = new HashMap<>(this.requiredFields);

        for (final DynamicField<B, R, ?> field : this.implicitFields.values()) {
            if (field.codec() == null) throw new UnreachableException();
            CodecUtils.asMapCodec(field.codec()).decode(ops, input)
                .ifSuccess((Object o) -> ((BiConsumer<B, Object>) field.setter()).accept(builder, o))
                .ifError(e -> errors.put(field.key(), e.messageSupplier()));
        }
        for (final DynamicField<B, R, ?> field : this.fields.values()) {
            if (field.isImplicit()) {
                continue;
            }
            final T value = input.get(field.key());
            if (value == null) {
                final var defaultResult = field.defaultSupplier().get();
                if (defaultResult != null) {
                    if (defaultResult.isSuccess()) {
                        ((BiConsumer<B, Object>) field.setter()).accept(builder, defaultResult.getOrThrow());
                    } else {
                        errors.put(field.key(), defaultResult.error().orElseThrow().messageSupplier());
                    }
                }
                if (field.isNullable()) {
                    field.setter().accept(builder, null);
                }
                continue;
            }
            required.remove(field.key());
            final DataResult<Object> result;
            if (field.codec() != null) { // parse normal field
                result = ((Codec<Object>) field.codec()).decode(ops, value).map(Pair::getFirst);
            } else { // parse recursive field
                result = ((MapCodec<Object>) this).compressedDecode(ops, value);
            }
            result.ifSuccess(o -> ((BiConsumer<B, Object>) field.setter()).accept(builder, o))
                .ifError(e -> errors.put(field.key(), e.messageSupplier()));
        }
        if (errors.isEmpty() && required.isEmpty()) {
            return DataResult.success(this.out.apply(builder));
        }
        return DataResult.error(() -> {
           final StringBuilder message = new StringBuilder();
           if (!required.isEmpty()) {
               message.append("Required fields are missing: ").append(required.keySet());
           }
           if (!errors.isEmpty()) {
               if (!message.isEmpty()) {
                   message.append(';');
               }
               errors.forEach((key, error) ->
                   message.append(key).append(": ").append(error.get()));
           }
           return message.toString();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input == null) {
            return prefix.withErrorsFrom(DataResult.error(() -> "Input is null"));
        }
        final R reader = this.in.apply(input);
        for (final DynamicField<B, R, ?> field : this.fields.values()) {
            final Object value = field.getter().apply(reader);
            if (value == null) {
                continue;
            }
            final BiPredicate<R, Object> filter = (BiPredicate<R, Object>) field.outputFilter();
            if (filter != null && !filter.test(reader, value)) {
                continue;
            }
            if (field.codec() != null) {
                if (field.isImplicit()) { // encode implicit field (multiple fields)
                    prefix = CodecUtils.asMapCodec((Codec<Object>) field.codec()).encode(value, ops, prefix);
                } else { // encode normal field
                    prefix.add(field.key(), ((Codec<Object>) field.codec()).encodeStart(ops, value));
                }
            } else { // encode recursive field
                prefix.add(field.key(), ((MapCodec<Object>) this).encode(value, ops, this.compressedBuilder(ops)).build(ops.empty()));
            }
        }
        return prefix;
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