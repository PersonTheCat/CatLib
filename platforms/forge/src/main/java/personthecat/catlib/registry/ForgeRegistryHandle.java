package personthecat.catlib.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class ForgeRegistryHandle<T extends IForgeRegistryEntry<T>> implements RegistryHandle<T> {

    private final ForgeRegistry<T> registry;

    public ForgeRegistryHandle(final ForgeRegistry<T> registry) {
        this.registry = registry;
    }

    public ForgeRegistry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public @Nullable ResourceLocation getKey(final T t) {
        return t.getRegistryName();
    }

    @Override
    public @Nullable T lookup(final ResourceLocation id) {
        return this.registry.getValue(id);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        this.registry.register(v.setRegistryName(id));
        return v;
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : new HashSet<>(this.registry.getEntries())) {
            f.accept(entry.getKey().location(), entry.getValue());
        }
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
        return this.registry.getHolderHelper().map(h -> h.tags).orElse(Collections.emptyMap());
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.registry.key;
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
    public Stream<T> stream() {
        return this.registry.getValues().stream();
    }
}
