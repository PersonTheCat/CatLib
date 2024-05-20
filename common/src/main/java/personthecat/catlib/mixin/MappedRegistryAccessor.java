package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor<T> {

    @Accessor("byKey")
    Map<ResourceKey<T>, Holder<T>> getHolderMap();

    @Accessor("tags")
    Map<TagKey<T>, HolderSet.Named<T>> getTagsDirectly();
}
