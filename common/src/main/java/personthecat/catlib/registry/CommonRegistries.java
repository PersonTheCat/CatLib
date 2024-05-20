package personthecat.catlib.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

@SuppressWarnings("unused")
public class CommonRegistries {
    public static final RegistryHandle<Block> BLOCKS = RegistryUtils.getHandle(Registries.BLOCK);
    public static final RegistryHandle<Fluid> FLUIDS = RegistryUtils.getHandle(Registries.FLUID);
    public static final RegistryHandle<Item> ITEMS = RegistryUtils.getHandle(Registries.ITEM);
    public static final RegistryHandle<EntityType<?>> ENTITIES = RegistryUtils.getHandle(Registries.ENTITY_TYPE);

    public static <T> RegistryHandle<T> get(final ResourceKey<Registry<T>> key) {
        return RegistryUtils.getHandle(key);
    }
}
