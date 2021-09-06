package personthecat.catlib.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import personthecat.catlib.event.registry.RegistryHandle;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(MappedRegistry.class)
public abstract class MojangRegistryMixin<T> extends Registry<T> implements RegistryHandle<T> {

    protected MojangRegistryMixin(ResourceKey<? extends Registry<T>> key, final Lifecycle lifecycle) {
        super(key, lifecycle);
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.get(id);
    }

    @Shadow
    @Nullable
    @Override
    public abstract ResourceLocation getKey(final T t);

    @Override
    public void register(final ResourceLocation id, final T t) {
        this.register(ResourceKey.create(this.key(), id), t, Lifecycle.stable());
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : this.entrySet()) {
            f.accept(entry.getKey().location(), entry.getValue());
        }
    }

    @Shadow
    @NotNull
    @Override
    public abstract Iterator<T> iterator();

    @Shadow
    public abstract <V extends T> V register(final ResourceKey<T> key, final V v, final Lifecycle lifecycle);
}
