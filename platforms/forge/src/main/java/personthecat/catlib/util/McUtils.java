package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.registries.ForgeRegistries;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.io.File;
import java.util.Optional;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McUtils {

    @Overwrite
    public static File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    @Overwrite
    public static String getPlatform() {
        return "forge";
    }

    @Overwrite
    public static boolean isModLoaded(final String id) {
        return ModList.get().isLoaded(id);
    }

    @Overwrite
    public static Optional<Block> getBlock(final ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id));
    }

    @Overwrite
    public static Iterable<Block> getAllBlocks() {
        return ForgeRegistries.BLOCKS;
    }

    @Overwrite
    public static boolean isDedicatedServer() {
        return Dist.DEDICATED_SERVER == Environment.get().getDist();
    }

    @Inherit
    public static boolean isClientSide() {
        return !isDedicatedServer();
    }

    @Overwrite
    public static Optional<Item> getItem(final ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(id));
    }

    @Overwrite
    public static Iterable<Item> getAllItems() {
        return ForgeRegistries.ITEMS;
    }
}
