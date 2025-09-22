package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.registry.RegistryUtils;
import xjs.data.Json;
import xjs.data.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CodecSupport {

    private static final Map<Class<?>, Function<Object, Codec<?>>> CODECS_BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, Codec<?>> CODECS_BY_KEY = new ConcurrentHashMap<>();

    private CodecSupport() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void registerGetter(final Class<T> clazz, final Function<? extends T, Codec<? extends T>> getter) {
        CODECS_BY_TYPE.put(clazz, (Function) getter);
    }

    public static <T> void registerCodec(final ResourceKey<T> key, final Codec<? extends T> codec) {
        CODECS_BY_KEY.put(key, codec);
    }

    /**
     * Stringifies the given object <em>if</em> it has a discernible {@link
     * Codec}
     *
     * @param o The object being stringified.
     * @return A string representation of the value.
     */
    public static @Nullable String anyToString(final Object o) {
        if (o == null) return "null";
        final JsonValue value = serializeAny(o);
        if (value == null) return null;
        return value.toString("djs");
    }

    /**
     * Converts the given object to a {@link JsonValue} <em>if</em> it has
     * as discernible {@link Codec}. Else, returns null.
     *
     * @param o The object being serialized.
     * @return A {@link JsonValue json} representation of the value.
     */
    public static @Nullable JsonValue serializeAny(final Object o) {
        if (o == null) return Json.value(null);
        final Codec<Object> c = tryGetCodec(o);
        if (c == null) return null;
        return c.encodeStart(XjsOps.INSTANCE, o).result().orElse(null);
    }

    /**
     * Attempts to resolve the {@link Codec} of the given value reflectively.
     * <p>
     * Some edge cases may support non-reflective access.
     *
     * @param t   The object being inspected.
     * @param <T> The type of object being inspected.
     * @return The {@link Codec codec}, else <code>null</code>.
     */
    @SuppressWarnings("ConstantConditions") // definitely does return null
    public static <T> @Nullable Codec<T> tryGetCodec(final T t) {
        if (t == null) return null;
        final Function<Object, Codec<?>> c = CODECS_BY_TYPE.computeIfAbsent(t.getClass(), clazz -> {
            final ResourceKey<? extends Registry<?>> key =
                RegistryUtils.tryGetByType(t.getClass()).map(RegistryHandle::key).orElse(null);
            final Codec<?> codec = tryGetCodec(key);
            return codec != null ? o -> codec : null;
        });
        return c != null ? cast(c.apply(t)) : null;
    }

    /**
     * Resolves a {@link Codec codec} for the given {@link ResourceKey key}, either
     * from the cache or directly from {@link RegistryDataLoader}.
     *
     * @param key The key of the registry corresponding to the codec.
     * @param <T> The type of registry and codec.
     * @return The codec itself, or else <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable Codec<T> tryGetCodec(final ResourceKey<? extends Registry<T>> key) {
        if (key == null) return null;
        return cast(CODECS_BY_KEY.computeIfAbsent(key, k -> getFromDataLoader((ResourceKey<? extends Registry<?>>) k)));
    }

    private static @Nullable Codec<?> getFromDataLoader(ResourceKey<? extends Registry<?>> key) {
        Codec<?> c = lookup(RegistryDataLoader.WORLDGEN_REGISTRIES, key);
        if (c != null) return c;
        c = lookup(RegistryDataLoader.DIMENSION_REGISTRIES, key);
        if (c != null) return c;
        return lookup(RegistryDataLoader.SYNCHRONIZED_REGISTRIES, key);
    }

    private static @Nullable Codec<?> lookup(
            final List<RegistryDataLoader.RegistryData<?>> registries, ResourceKey<? extends Registry<?>> key) {
        for (final RegistryDataLoader.RegistryData<?> data : registries) {
            if (data.key().equals(key)) {
                return data.elementCodec();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> cast(final Codec<?> codec) {
        return (Codec<T>) codec;
    }
}
