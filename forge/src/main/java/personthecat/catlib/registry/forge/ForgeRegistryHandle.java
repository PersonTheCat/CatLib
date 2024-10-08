package personthecat.catlib.registry.forge;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NamespacedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ForgeRegistryHandle<T> implements RegistryHandle<T> {

    private final ForgeRegistry<T> registry;
    private final Lookup lookup = new Lookup();

    public ForgeRegistryHandle(final IForgeRegistry<T> registry) {
        this(unwrap(registry));
    }

    public ForgeRegistryHandle(final ForgeRegistry<T> registry) {
        this.registry = registry;
    }

    public static <T> ForgeRegistry<T> unwrap(final IForgeRegistry<T> registry) {
        if (registry instanceof ForgeRegistry<T> f) {
            return f;
        }
        throw new IllegalArgumentException("Nonstandard Forge config: " + registry.getClass().getSimpleName());
    }

    public ForgeRegistry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public @Nullable ResourceLocation getKey(final T t) {
        return this.registry.getKey(t);
    }

    @Override
    public @Nullable T lookup(final ResourceLocation id) {
        return this.registry.getValue(id);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        this.registry.register(id, v);
        return v;
    }

    @Override
    public <V extends T> void deferredRegister(final String modId, final ResourceLocation id, final V v) {
        MojangRegistryHandleImpl.doDeferredRegister(this.registry.key, modId, id, v);
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : new HashSet<>(this.registry.getEntries())) {
            f.accept(entry.getKey().location(), entry.getValue());
        }
    }

    @Override
    public void forEachHolder(final BiConsumer<ResourceLocation, Holder<T>> f) {
        this.holders().forEach(holder -> f.accept(this.keyOf(holder), holder));
    }

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return this.registry.containsKey(id);
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceLocation id) {
        return this.registry.getHolder(id).orElse(null);
    }

    @Override
    public Map<TagKey<T>, HolderSet.Named<T>> getTags() {
        final NamespacedWrapper<T> wrapper = this.registry.getWrapper();
        if (wrapper == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(wrapper.tags);
    }

    @Override
    @Nullable
    public HolderSet.Named<T> getNamed(final TagKey<T> key) {
        final NamespacedWrapper<T> wrapper = this.registry.getWrapper();
        return wrapper != null ? wrapper.tags.get(key) : null;
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.registry.key;
    }

    @Override
    public Collection<? extends Holder<T>> holders() {
        final NamespacedWrapper<T> wrapper = this.registry.getWrapper();
        if (wrapper == null) return Collections.emptySet();
        return wrapper.holders().toList();
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return this.registry.getKeys();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return this.registry.getEntries();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return this.registry.iterator();
    }

    @Override
    public HolderLookup<T> asLookup() {
        return this.lookup;
    }

    @Override
    public Stream<T> stream() {
        return this.registry.getValues().stream();
    }

    @Override
    public Codec<Holder<T>> holderCodec() {
        return this.codec().comapFlatMap(this::tryGetHolder, Holder::value);
    }

    @Override
    public Codec<T> codec() {
        return this.registry.getCodec();
    }

    @Override
    public int size() {
        return this.registry.getValues().size();
    }

    private class Lookup implements HolderLookup<T>, HolderOwner<T> {
        @Override
        @NotNull
        public Stream<Holder.Reference<T>> listElements() {
            return holders().stream().map(holder -> (Holder.Reference<T>) holder);
        }

        @Override
        @NotNull
        public Stream<HolderSet.Named<T>> listTags() {
            return getTags().values().stream();
        }

        @Override
        @NotNull
        public Optional<Holder.Reference<T>> get(final @NotNull ResourceKey<T> key) {
            return registry.getHolder(key).map(holder -> (Holder.Reference<T>) holder);
        }

        @Override
        @NotNull
        public Optional<HolderSet.Named<T>> get(final @NotNull TagKey<T> arg) {
            return Optional.ofNullable(getNamed(arg));
        }
    }
}
