package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.BiomeType;
import personthecat.catlib.data.IdList;
import personthecat.catlib.exception.JsonFormatException;
import personthecat.catlib.util.LibUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class CodecUtils {

    public static final ValueMapCodec<String> STRING_MAP = mapOf(Codec.STRING);
    public static final ValueMapCodec<Boolean> BOOLEAN_MAP = mapOf(Codec.BOOL);
    public static final ValueMapCodec<Integer> INT_MAP = mapOf(Codec.INT);
    public static final ValueMapCodec<Float> FLOAT_MAP = mapOf(Codec.FLOAT);
    public static final Codec<List<String>> STRING_LIST = easyList(Codec.STRING);
    public static final Codec<List<Integer>> INT_LIST = easyList(Codec.INT);
    public static final Codec<List<Float>> FLOAT_LIST = easyList(Codec.FLOAT);
    public static final Codec<List<BiomeType>> CATEGORY_LIST = easyList(ofEnum(BiomeType.class));
    @Deprecated public static final Codec<List<ResourceLocation>> ID_LIST = easyList(ResourceLocation.CODEC);

    public static <A> ValueMapCodec<A> mapOf(final Codec<A> codec) {
        return new ValueMapCodec<>(codec);
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

    public static <A> SetCodec<A> setOf(final @NotNull Codec<A> codec) {
        return new SetCodec<>(codec);
    }

    public static <A> Codec<Set<A>> easySet(final @NotNull Codec<A> codec) {
        return Codec.either(codec, setOf(codec)).xmap(
            either -> either.map(Collections::singleton, Function.identity()),
            set -> {
                if (set == null) return Either.right(Collections.emptySet());
                return set.size() == 1 ? Either.left(set.iterator().next()) : Either.right(set);
            }
        );
    }

    public static <A> Codec<IdList<A>> idList(final ResourceKey<? extends Registry<A>> key) {
        return IdList.codecOf(key);
    }

    public static <A> Codec<List<A>> autoFlatten(final @NotNull Codec<A> codec) {
        return new AutoFlatListCodec<>(codec);
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

    public static <T> SimpleEitherCodec<T> simpleEither(final Codec<T> first, final Codec<T> second) {
        return new SimpleEitherCodec<>(first, second);
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

    public static <A, T> Optional<A> readOptional(final Codec<A> codec, final DynamicOps<T> ops, final T prefix) {
        return codec.parse(ops, prefix).result();
    }

    public static <A, T> A readThrowing(final Codec<A> codec, final DynamicOps<T> ops, final T prefix) {
        return codec.parse(ops, prefix).getOrThrow(JsonFormatException::new);
    }

    public static <T> EasyMapReader<T> easyReader(final DynamicOps<T> ops, final T prefix) {
        return new EasyMapReader<>(ops, prefix);
    }

    public static <B> DynamicCodec.Builder<B, B, B> dynamic(final Supplier<B> builder) {
        return new DynamicCodec.Builder<>(builder, Function.identity(), Function.identity());
    }

    public static <B, A> DynamicCodec.Builder<B, A, A> dynamic(final Supplier<B> builder, final Function<B, A> out) {
        return new DynamicCodec.Builder<>(builder, Function.identity(), out);
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

    public static <O, T1> MapCodec<O> codecOf(RecordCodecBuilder<O, T1> t1, Function<T1, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1).apply(i, f));
    }

    public static <O, T1, T2> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, BiFunction<T1, T2, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2).apply(i, f));
    }

    public static <O, T1, T2, T3> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            Function3<T1, T2, T3, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3).apply(i, f));
    }

    public static <O, T1, T2, T3, T4> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, Function4<T1, T2, T3, T4, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, Function5<T1, T2, T3, T4, T5, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            Function6<T1, T2, T3, T4, T5, T6, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, Function7<T1, T2, T3, T4, T5, T6, T7, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, Function8<T1, T2, T3, T4, T5, T6, T7, T8, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11,
            Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11, RecordCodecBuilder<O, T12> t12,
            Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11, RecordCodecBuilder<O, T12> t12,
            RecordCodecBuilder<O, T13> t13, Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11, RecordCodecBuilder<O, T12> t12,
            RecordCodecBuilder<O, T13> t13, RecordCodecBuilder<O, T14> t14,
            Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11, RecordCodecBuilder<O, T12> t12,
            RecordCodecBuilder<O, T13> t13, RecordCodecBuilder<O, T14> t14, RecordCodecBuilder<O, T15> t15,
            Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15).apply(i, f));
    }

    public static <O, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> MapCodec<O> codecOf(
            RecordCodecBuilder<O, T1> t1, RecordCodecBuilder<O, T2> t2, RecordCodecBuilder<O, T3> t3,
            RecordCodecBuilder<O, T4> t4, RecordCodecBuilder<O, T5> t5, RecordCodecBuilder<O, T6> t6,
            RecordCodecBuilder<O, T7> t7, RecordCodecBuilder<O, T8> t8, RecordCodecBuilder<O, T9> t9,
            RecordCodecBuilder<O, T10> t10, RecordCodecBuilder<O, T11> t11, RecordCodecBuilder<O, T12> t12,
            RecordCodecBuilder<O, T13> t13, RecordCodecBuilder<O, T14> t14, RecordCodecBuilder<O, T15> t15,
            RecordCodecBuilder<O, T16> t16, Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, O> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16).apply(i, f));
    }
}
