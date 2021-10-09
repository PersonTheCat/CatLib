package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import personthecat.catlib.util.RegistryUtils;

@SuppressWarnings("unused")
public class CommonRegistries {
    public static final RegistryHandle<Block> BLOCKS = RegistryUtils.getHandle(Registry.BLOCK_REGISTRY);
    public static final RegistryHandle<Fluid> FLUIDS = RegistryUtils.getHandle(Registry.FLUID_REGISTRY);
    public static final RegistryHandle<Item> ITEMS = RegistryUtils.getHandle(Registry.ITEM_REGISTRY);
    public static final RegistryHandle<EntityType<?>> ENTITIES = RegistryUtils.getHandle(Registry.ENTITY_TYPE_REGISTRY);
}
