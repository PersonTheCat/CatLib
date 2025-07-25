package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function13;
import com.mojang.datafixers.util.Function14;
import com.mojang.datafixers.util.Function15;
import com.mojang.datafixers.util.Function16;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.IdList;
import personthecat.catlib.util.LibUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CodecUtils {

    public static <A> Codec<Map<String, A>> mapOf(final Codec<A> codec) {
        return Codec.unboundedMap(Codec.STRING, codec);
    }

    public static <A> Codec<List<A>> easyList(final @NotNull Codec<A> codec) {
        return Codec.either(codec, codec.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            list -> {
                if (list == null) return Either.right(Collections.emptyList());
                return list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list);
            }
        );
    }

    public static <A> Codec<IdList<A>> idList(final ResourceKey<Registry<A>> key) {
        return IdList.codecOf(key);
    }

    public static <E extends Enum<E>> Codec<E> ofEnum(final Class<E> e) {
        final StringBuilder sb = new StringBuilder("[");
        for (final E constant : e.getEnumConstants()) {
            sb.append(constant.name()).append(',');
        }
        final String names = sb.append(']').toString();

        return Codec.STRING.flatXmap(
            name -> {
                final E c = LibUtil.getEnumConstant(name, e).orElse(null);
                return c == null ? DataResult.error(() -> "Unknown key: " + name + ". Expected one of " + names) : DataResult.success(c);
            },
            constant -> DataResult.success(constant.name())
        );
    }

    public static <A, S> Codec<S> xmapWithOps(
            final Codec<A> codec,
            final BiFunction<? super DynamicOps<?>, ? super A, ? extends S> to,
            final BiFunction<? super DynamicOps<?>, ? super S, ? extends A> from) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<S, T>> decode(final DynamicOps<T> ops, final T input) {
                return codec.decode(ops, input).map(pair -> Pair.of(to.apply(ops, pair.getFirst()), pair.getSecond()));
            }

            @Override
            public <T> DataResult<T> encode(final S input, final DynamicOps<T> ops, final T prefix) {
                return codec.encode(from.apply(ops, input), ops, prefix);
            }

            @Override
            public String toString() {
                return codec.toString() + "[xmapped]";
            }
        };
    }

    @SafeVarargs
    public static <T> SimpleAnyCodec<T> simpleAny(final Decoder<? extends T> first, final Decoder<? extends T>... others) {
        return new SimpleAnyCodec<>(first, others);
    }

    public static <T> SimpleEitherCodec<T> simpleEither(final Decoder<T> first, final Decoder<T> second) {
        return new SimpleEitherCodec<>(first, second);
    }

    public static <T> Codec<T> defaultType(final Codec<T> dispatcher, final MapCodec<? extends T> defaultType) {
        return defaultType("type", dispatcher, defaultType, (t, ops) -> false);
    }

    public static <T> Codec<T> defaultType(final String typeKey, final Codec<T> dispatcher, final MapCodec<? extends T> defaultType) {
        return defaultType(typeKey, dispatcher, defaultType, (t, ops) -> false);
    }
    
    public static <T> Codec<T> defaultType(final Codec<T> dispatcher, final MapCodec<? extends T> defaultType, final BiPredicate<DynamicOps<?>, T> isDefaultType) {
        return defaultType("type", dispatcher, defaultType, isDefaultType);
    }

    public static <T> Codec<T> defaultType(final String typeKey, final Codec<T> dispatcher, final MapCodec<? extends T> defaultType, final BiPredicate<DynamicOps<?>, T> isDefaultType) {
        return defaultType(
                typeKey, dispatcher, (ops, map) -> DataResult.success(defaultType), (ops, map) -> DataResult.success(defaultType))
            .filterEncoder(isDefaultType);
    }

    public static <T> DefaultTypeCodec<T> defaultType(
            final String typeKey,
            final Codec<T> dispatcher,
            final BiFunction<DynamicOps<?>, MapLike<?>, DataResult<? extends MapDecoder<? extends T>>> defaultDecoder,
            final BiFunction<DynamicOps<?>, ? super T, DataResult<? extends MapEncoder<? extends T>>> defaultEncoder) {
        return new DefaultTypeCodec<>(typeKey, dispatcher, defaultDecoder, defaultEncoder);
    }

    public static <T> MapCodec<T> filter(final MapCodec<T> codec, final Predicate<T> filter) {
        return new FilteredMapCodec<>(codec, filter);
    }

    public static <T> Codec<T> ifMap(final Codec<T> codec, final MapCodec<T> map) {
        return ifMap(codec, map, (t, ops) -> false);
    }

    public static <T> Codec<T> ifMap(final Codec<T> codec, final MapCodec<T> map, final BiPredicate<T, DynamicOps<?>> isMap) {
        return new IfMapCodec<>(codec, map, isMap);
    }

    public static <T> MapCodec<T> asMapCodec(final Codec<T> codec) {
        return codec instanceof MapCodec.MapCodecCodec<T> map ? map.codec() : MapCodec.assumeMapUnsafe(codec);
    }

    public static <T> Codec<T> toCodecUnsafe(final Decoder<T> decoder) {
        if (decoder instanceof Codec<T> codec) {
            return codec;
        }
        return Codec.of(neverCodec(), decoder);
    }

    public static <T> TypedCodec<T> typed(final Codec<T> codec, final Class<T> type) {
        return TypedCodec.of(codec, type);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> TypedCodec<T> typed(final Codec<T> codec, final T... implicitTypeArg) {
        return TypedCodec.of(codec, (Class<T>) implicitTypeArg.getClass().getComponentType());
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> asParent(final Codec<? extends T> codec) {
        return (Codec<T>) codec;
    }

    @SuppressWarnings("unchecked")
    public static <T> MapCodec<T> asParent(final MapCodec<? extends T> codec) {
        return (MapCodec<T>) codec;
    }

    @SuppressWarnings("unchecked")
    public static <T> DataResult<T> asParent(final DataResult<? extends T> result) {
        return (DataResult<T>) result;
    }

    public static <T, R> Codec<R> cast(final Codec<T> codec, final Class<T> t, final Class<R> r) {
        return codec.flatXmap(ta -> tryCast(ta, r), ra -> tryCast(ra, t));
    }

    private static <T, R> Codec<R> cast(
            final Codec<T> codec, final Class<T> t, final Class<R> r, final String tErr, final String rErr) {
        return codec.flatXmap(
            ta -> tryCast(ta, r, ta2 -> String.format(tErr, r, ta2)),
            ra -> tryCast(ra, t, ra2 -> String.format(rErr, t, ra2)));
    }

    public static <T, R> DataResult<R> tryCast(final T t, final Class<R> r) {
        return tryCast(t, r, a -> "Not an instance of " + r.getSimpleName() + ": " + t);
    }

    public static <T, R> DataResult<R> tryCast(final T t, final Class<R> r, final Function<T, String> error) {
        return r.isInstance(t) ? DataResult.success(r.cast(t)) : DataResult.error(() -> error.apply(t));
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> neverCodec() {
        return (Codec<T>) NeverCodec.INSTANCE;
    }

    public static <T> MapCodec<T> neverMapCodec() {
        return MapCodec.assumeMapUnsafe(neverCodec());
    }

    public static <T> Codec<Optional<T>> optionalCodec(final Codec<T> codec) {
        return new OptionalCodec<>(codec);
    }

    public static <B> DynamicCodec.Builder<B, B, B> dynamic(final Supplier<B> builder) {
        return dynamic(builder, Function.identity());
    }

    public static <B, A> DynamicCodec.Builder<B, A, A> dynamic(final Supplier<B> builder, final Function<B, A> out) {
        return dynamic(builder, Function.identity(), out);
    }

    public static <B, R, A> DynamicCodec.Builder<B, R, A> dynamic(final Supplier<B> builder, final Function<A, R> in, final Function<B, A> out) {
        return new DynamicCodec.Builder<>(builder, in, out);
    }

    public static <O, T1, R1> MapCodec<O> codecOf(FieldDescriptor<O, T1, R1> f1, Function<R1, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f).apply(i, t1 -> c.apply(f1.r(t1))));
    }

    public static <O, T1, R1, T2, R2> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, BiFunction<R1, R2, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f).apply(i, (t1, t2) -> c.apply(f1.r(t1), f2.r(t2))));
    }

    public static <O, T1, R1, T2, R2, T3, R3> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            Function3<R1, R2, R3, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f)
            .apply(i, (t1, t2, t3) -> c.apply(f1.r(t1), f2.r(t2), f3.r(t3))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, Function4<R1, R2, R3, R4, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f)
            .apply(i, (t1, t2, t3, t4) -> c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, Function5<R1, R2, R3, R4, R5, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f)
            .apply(i, (t1, t2, t3, t4, t5) -> c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            Function6<R1, R2, R3, R4, R5, R6, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f)
            .apply(i, (t1, t2, t3, t4, t5, t6) -> c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, Function7<R1, R2, R3, R4, R5, R6, R7, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8,
            Function8<R1, R2, R3, R4, R5, R6, R7, R8, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            Function9<R1, R2, R3, R4, R5, R6, R7, R8, R9, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, Function10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, 
            Function11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11, T12, R12> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, FieldDescriptor<O, T12, R12> f12,
            Function12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f, f12.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11), f12.r(t12))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11, T12, R12, T13, R13> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, FieldDescriptor<O, T12, R12> f12,
            FieldDescriptor<O, T13, R13> f13, Function13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f, f12.f, f13.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11), f12.r(t12), f13.r(t13))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11, T12, R12, T13, R13, T14, R14> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, FieldDescriptor<O, T12, R12> f12,
            FieldDescriptor<O, T13, R13> f13, FieldDescriptor<O, T14, R14> f14,
            Function14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f, f12.f, f13.f, f14.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11), f12.r(t12), f13.r(t13), f14.r(t14))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11, T12, R12, T13, R13, T14, R14, T15, R15> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, FieldDescriptor<O, T12, R12> f12,
            FieldDescriptor<O, T13, R13> f13, FieldDescriptor<O, T14, R14> f14, FieldDescriptor<O, T15, R15> f15,
            Function15<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f, f12.f, f13.f, f14.f, f15.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11), f12.r(t12), f13.r(t13), f14.r(t14), f15.r(t15))));
    }

    public static <O, T1, R1, T2, R2, T3, R3, T4, R4, T5, R5, T6, R6, T7, R7, T8, R8, T9, R9, T10, R10, T11, R11, T12, R12, T13, R13, T14, R14, T15, R15, T16, R16> MapCodec<O> codecOf(
            FieldDescriptor<O, T1, R1> f1, FieldDescriptor<O, T2, R2> f2, FieldDescriptor<O, T3, R3> f3,
            FieldDescriptor<O, T4, R4> f4, FieldDescriptor<O, T5, R5> f5, FieldDescriptor<O, T6, R6> f6,
            FieldDescriptor<O, T7, R7> f7, FieldDescriptor<O, T8, R8> f8, FieldDescriptor<O, T9, R9> f9,
            FieldDescriptor<O, T10, R10> f10, FieldDescriptor<O, T11, R11> f11, FieldDescriptor<O, T12, R12> f12,
            FieldDescriptor<O, T13, R13> f13, FieldDescriptor<O, T14, R14> f14, FieldDescriptor<O, T15, R15> f15,
            FieldDescriptor<O, T16, R16> f16, Function16<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15, R16, O> c) {
        return RecordCodecBuilder.mapCodec(i -> i.group(f1.f, f2.f, f3.f, f4.f, f5.f, f6.f, f7.f, f8.f, f9.f, f10.f, f11.f, f12.f, f13.f, f14.f, f15.f, f16.f)
            .apply(i, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16) ->
                c.apply(f1.r(t1), f2.r(t2), f3.r(t3), f4.r(t4), f5.r(t5), f6.r(t6), f7.r(t7), f8.r(t8), f9.r(t9), f10.r(t10), f11.r(t11), f12.r(t12), f13.r(t13), f14.r(t14), f15.r(t15), f16.r(t16))));
    }

    private static class NeverCodec implements Codec<Object> {
        private static final NeverCodec INSTANCE = new NeverCodec();

        @Override
        public <T> DataResult<T> encode(final Object input, final DynamicOps<T> ops, final T prefix) {
            return DataResult.error(() -> "Not an encoder");
        }

        @Override
        public <T> DataResult<Pair<Object, T>> decode(final DynamicOps<T> ops, final T input) {
            return DataResult.error(() -> "Not a decoder");
        }

        @Override
        public String toString() {
            return "NEVER";
        }
    }
}
