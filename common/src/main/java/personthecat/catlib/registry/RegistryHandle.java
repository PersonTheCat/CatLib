package personthecat.catlib.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public interface RegistryHandle<T> extends Iterable<T> {
    @Nullable ResourceKey<T> getKey(final T t);
    @Nullable T lookup(final ResourceKey<T> key);
    <V extends T> V register(final ResourceKey<T> key, V v);
    <V extends T> void deferredRegister(final String modId, final ResourceLocation id, V v);
    void forEach(final BiConsumer<ResourceKey<T>, T> f);
    void forEachHolder(final BiConsumer<ResourceKey<T>, Holder<T>> f);
    boolean isRegistered(final ResourceKey<T> key);
    @Nullable Holder<T> getHolder(final ResourceKey<T> key);
    Map<TagKey<T>, HolderSet.Named<T>> getTags();
    ResourceKey<? extends Registry<T>> key();
    Collection<? extends Holder<T>> holders();
    Set<ResourceKey<T>> keySet();
    Set<Map.Entry<ResourceKey<T>, T>> entrySet();
    HolderLookup<T> asLookup();
    Stream<T> stream();
    Codec<Holder<T>> holderCodec();
    Codec<T> codec();
    int size();

    static <T> RegistryHandle<T> createAndRegister(final ModDescriptor mod, final ResourceKey<Registry<T>> key) {
        final var handle = create(key);
        addToRoot(mod, handle);
        return handle;
    }

    @ExpectPlatform
    static <T> RegistryHandle<T> create(final ResourceKey<Registry<T>> key) {
        return new MojangRegistryHandle<>(new MappedRegistry<>(key, Lifecycle.stable()));
    }

    @ExpectPlatform
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> void addToRoot(final ModDescriptor mod, final RegistryHandle<T> handle) {
        if (handle instanceof MojangRegistryHandle<T> m) { // platforms will handle other types, events, etc
            Registry.register((Registry) BuiltInRegistries.REGISTRY, (ResourceKey) m.key(), m.getRegistry());
        }
    }

    @ExpectPlatform
    static <T> RegistryHandle<T> createDynamic(
            final ModDescriptor mod, final ResourceKey<Registry<T>> key, final Codec<T> elementCodec) {
        return DynamicRegistryHandle.createHandle(key); // platforms will handle events, create as data pack registry.
    }

    default ResourceLocation getId(final T t) {
        final var k = this.getKey(t);
        return k != null ? k.location() : null;
    }

    default boolean isRegistered(ResourceLocation id) {
        final var k = createKey(this.key(), id);
        return k != null && this.isRegistered(k);
    }

    default @Nullable T lookup(final ResourceLocation id) {
        final var k = createKey(this.key(), id);
        return k != null ? this.lookup(k) : null;
    }

    default @Nullable Holder<T> getHolder(final ResourceLocation id) {
        final var k = createKey(this.key(), id);
        return k != null ? this.getHolder(k) : null;
    }

    default <V extends T> V register(final ResourceLocation id, V v) {
        return this.register(createKey(this.key(), id), v);
    }

    default DeferredRegister<T> createRegister(final String modId) {
        return new DeferredRegister<>(this, modId);
    }

    default <V extends T> void deferredRegister(final String modId, final String id, final V v) {
        this.deferredRegister(modId, new ResourceLocation(modId, id), v);
    }

    default <V extends T> void deferredRegister(final ResourceLocation id, final V v) {
        this.deferredRegister(id.getNamespace(), id, v);
    }

    default @Nullable ResourceKey<T> keyOf(final Holder<T> holder) {
        return holder.unwrapKey().orElseGet(() -> this.getKey(holder.value()));
    }

    default @Nullable ResourceLocation idOf(final Holder<T> holder) {
        final var k = this.keyOf(holder);
        return k != null ? k.location() : null;
    }

    default @Nullable HolderSet.Named<T> getNamed(final TagKey<T> key) {
        return this.getTags().get(key);
    }

    default @Nullable Holder<T> getHolder(final T t) {
        final var k = this.getKey(t);
        return k != null ? this.getHolder(k) : null;
    }

    default DataResult<Holder<T>> tryGetHolder(final T t) {
        return Optional.ofNullable(this.getHolder(t))
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unregistered value (no key): " + t));
    }

    default Collection<T> getTag(final TagKey<T> key) {
        final HolderSet.Named<T> named = this.getNamed(key);
        if (named == null) return Collections.emptyList();
        return named.stream().map(Holder::value).toList();
    }

    default Collection<T> getTag(final ResourceLocation id) {
        final ResourceKey<? extends Registry<T>> key = this.key();
        return key != null ? this.getTag(TagKey.create(key, id)) : Collections.emptySet();
    }

    default Registry<T> asRegistry() {
        throw new UnsupportedOperationException("Not a Mojang registry: " + this);
    }

    default boolean isEmpty() {
        return this.size() == 0;
    }

    default Stream<ResourceLocation> streamIds() {
        return this.streamKeys().map(ResourceKey::location);
    }

    default Stream<ResourceKey<T>> streamKeys() {
        return this.keySet().stream();
    }

    default Stream<Map.Entry<ResourceKey<T>, T>> streamEntries() {
        return this.entrySet().stream();
    }

    default int getId() {
        return System.identityHashCode(this);
    }

    private static <T> @Nullable ResourceKey<T> createKey(
            final @Nullable ResourceKey<? extends Registry<T>> key, final ResourceLocation id) {
        return key != null ? ResourceKey.create(key, id) : null;
    }

    record DeferredRegister<T>(RegistryHandle<T> handle, String modId) {
        public <V extends T> DeferredRegister<T> register(String id, V v) {
            this.handle.deferredRegister(this.modId, id, v);
            return this;
        }
    }
}
