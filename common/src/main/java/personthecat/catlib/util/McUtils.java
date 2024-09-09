package personthecat.catlib.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.BiomeType;
import personthecat.catlib.exception.BiomeTypeNotFoundException;
import personthecat.catlib.exception.MissingOverrideException;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static personthecat.catlib.exception.Exceptions.noBiomeTypeNamed;
import static personthecat.catlib.util.LibUtil.getEnumConstant;

@SuppressWarnings("unused")
public final class McUtils {

    private McUtils() {}

    /**
     * @return A {@link File} pointing to the game's config directory.
     */
    @ExpectPlatform
    public static File getConfigDir() {
        throw new MissingOverrideException();
    }

    /**
     * @return The name of the current platform, e.g. <code>forge</code>.
     */
    @ExpectPlatform
    public static String getPlatform() {
        throw new MissingOverrideException();
    }

    /**
     * @param id The id of the mod being researched.
     * @return Whether the given mod is currently installed.
     */
    @ExpectPlatform
    public static boolean isModLoaded(final String id) {
       throw new MissingOverrideException();
    }

    /**
     * @return Whether the game is currently running on a dedicated server.
     */
    @ExpectPlatform
    public static boolean isDedicatedServer() {
        throw new MissingOverrideException();
    }

    /**
     * @return Whether the game is currently running on the client side.
     */
    public static boolean isClientSide() {
        return !isDedicatedServer();
    }

    /**
     * @throws BiomeTypeNotFoundException If the biome type does not exist.
     * @param id The name of the biome type being researched.
     * @return The given biome type, or else throws.
     */
    @NotNull
    public static BiomeType assertBiomeType(final String id) {
        return getBiomeType(id).orElseThrow(() -> noBiomeTypeNamed(id));
    }

    /**
     * @param id The name of the biome type being researched.
     * @return The given biome type, or else {@link Optional#empty}.
     */
    public static Optional<BiomeType> getBiomeType(final String id) {
        return getEnumConstant(id, BiomeType.class);
    }

    /**
     * @param type The type of biome being researched.
     * @return A list of biomes for the given category.
     */
    public static List<Holder<Biome>> getBiomes(final BiomeType type) {
        return type.getTag().stream().toList();
    }
}
