package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

public class ForgeRegistryHandle<T extends IForgeRegistryEntry<T>> implements RegistryHandle<T> {

    private final ForgeRegistry<T> registry;

    public ForgeRegistryHandle(final ForgeRegistry<T> registry) {
        this.registry = registry;
    }

    public ForgeRegistry<T> getRegistry() {
        return this.registry;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(final T t) {
        return this.registry.getKey(t);
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.registry.getValue(id);
    }

    @Override
    public void register(final ResourceLocation id, final T t) {
        this.registry.register(t.setRegistryName(id));
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : new HashSet<>(this.registry.getEntries())) {
            f.accept(entry.getKey().location(), entry.getValue());
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.registry.iterator();
    }
}