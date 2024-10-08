package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class EasyMapReader<T> {

    private final DynamicOps<T> ops;
    private final MapLike<T> map;

    public EasyMapReader(final DynamicOps<T> ops, final MapLike<T> map) {
        this.ops = ops;
        this.map = map;
    }

    public <A> DataResult<A> run(final Function<Context<T>, A> runner) {
        final Context<T> ctx = new Context<>(this.ops, this.map);
        try {
            return DataResult.success(runner.apply(ctx));
        } catch (final EasyMapException e) {
            return DataResult.error(e::getMessage);
        }
    }

    public <A> DataResult<Pair<A, T>> runPaired(final Function<Context<T>, A> runner) {
        return run(ctx -> Pair.of(runner.apply(ctx), this.ops.createList(Stream.empty())));
    }

    public static class Context<T> {
        private final DynamicOps<T> ops;
        private final MapLike<T> map;
        private Class<? extends T> listClass;
        private Class<? extends T> mapClass;

        private Context(final DynamicOps<T> ops, final MapLike<T> map) {
            this.ops = ops;
            this.map = map;
        }

        @Nullable
        public T get(final String key) {
            return this.map.get(key);
        }

        public T mapOrNew(final String key) {
            // Invalid type will be handled later
            final T map = this.map.get(key);
            return map == null ? this.ops.emptyMap() : map;
        }

        public T easyListOrNew(final String key) {
            final T list = this.easyList(key);
            return list == null ? this.ops.emptyList() : list;
        }

        @Nullable
        public T easyList(final String key) {
            final T list = this.map.get(key);
            if (list == null) return null;
            return asOrToList(list);
        }

        private T asOrToList(final T list) {
            if (list.getClass() != this.getListClass()) {
                final T newList = this.ops.emptyList();
                final DataResult<T> result = this.ops.mergeToList(newList, list);
                if (result.error().isPresent()) {
                    throw new EasyMapException("Creating list: " + result.error().get().message());
                }
                assert result.result().isPresent();
                return result.result().get();
            }
            return list;
        }

        public <A> A readThis(final MapCodec<A> codec) {
            return this.getOrThrow(codec, this.map);
        }

        public boolean readBool(final String key) {
            return this.read(Codec.BOOL, key);
        }

        public boolean readBool(final String key, final Supplier<Boolean> def) {
            return this.read(Codec.BOOL, key, def);
        }

        public double readDouble(final String key) {
            return this.read(Codec.DOUBLE, key);
        }

        public double readDouble(final String key, final Supplier<Double> def) {
            return this.read(Codec.DOUBLE, key, def);
        }

        public float readFloat(final String key) {
            return this.read(Codec.FLOAT, key);
        }

        public float readFloat(final String key, final Supplier<Float> def) {
            return this.read(Codec.FLOAT, key, def);
        }

        public int readInt(final String key) {
            return this.read(Codec.INT, key);
        }

        public int readInt(final String key, final Supplier<Integer> def) {
            return this.read(Codec.INT, key, def);
        }

        public String readString(final String key) {
            return this.read(Codec.STRING, key);
        }

        public String readString(final String key, final Supplier<String> def) {
            return this.read(Codec.STRING, key, def);
        }

        public <A> A read(final Codec<A> codec, final String key) {
            final T t = this.map.get(key);
            if (t == null) throw this.missingValue(key);
            return this.getOrThrow(codec, t);
        }

        public <A> A read(final Codec<A> codec, final String key, final Supplier<A> def) {
            final T t = this.map.get(key);
            if (t == null) return def.get();
            return this.getOrThrow(codec, t);
        }

        public <A> Optional<A> readOptional(final Codec<A> codec, final String key) {
            final T t = this.map.get(key);
            if (t == null) return Optional.empty();
            return Optional.of(this.getOrThrow(codec, t));
        }

        public <A> A error(final String message) {
            throw new EasyMapException(message);
        }

        @SuppressWarnings("unchecked")
        private Class<? extends T> getListClass() {
            if (this.listClass == null) {
                this.listClass = (Class<? extends T>) this.ops.emptyList().getClass();
            }
            return this.listClass;
        }

        @SuppressWarnings("unchecked")
        private Class<? extends T> getMapClass() {
            if (this.mapClass == null) {
                this.mapClass = (Class<? extends T>) this.ops.emptyMap().getClass();
            }
            return this.mapClass;
        }

        private EasyMapException missingValue(final String key) {
            return new EasyMapException("Missing value: " + key);
        }

        private <A> A getOrThrow(final Codec<A> codec, final T t) {
            return codec.parse(this.ops, t).getOrThrow(EasyMapException::new);
        }

        private <A> A getOrThrow(final MapCodec<A> codec, final MapLike<T> map) {
            return codec.decode(this.ops, map).getOrThrow(EasyMapException::new);
        }
    }

    private static class EasyMapException extends RuntimeException {
        EasyMapException(final String msg) {
            super(msg);
        }
    }
}
