package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.registries.ForgeRegistries;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McTools {

    @Overwrite
    public static File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
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
    public static boolean isDedicatedServer() {
        return Dist.DEDICATED_SERVER == Environment.get().getDist();
    }

    @Overwrite
    public static Optional<Item> getItem(final ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(id));
    }

    @Overwrite
    public static Optional<Biome> getBiome(final ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.BIOMES.getValue(id));
    }

    @Overwrite
    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        return Stream.of(ForgeRegistries.BIOMES)
            .flatMap(reg -> reg.getValues().stream())
            .filter(b -> type.equals(b.getBiomeCategory()))
            .collect(Collectors.toList());
    }
}
