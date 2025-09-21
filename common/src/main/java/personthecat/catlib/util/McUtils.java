package personthecat.catlib.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.MissingOverrideException;

import java.nio.file.Path;
import java.util.Optional;

public final class McUtils {

    private McUtils() {}

    /**
     * @return A {@link Path} pointing to the game's config directory.
     */
    @ExpectPlatform
    public static Path getConfigDir() {
        throw new MissingOverrideException();
    }

    /**
     * @return The name of the current platform, e.g. <code>forge</code>.
     */
    @ExpectPlatform
    public static ModPlatform getPlatform() {
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
     * @param id The id of the mod being researched.
     * @return Information from the mod platform for the given mod ID.
     */
    @ExpectPlatform
    public static Optional<ModDescriptor> getMod(String id) {
        throw new MissingOverrideException();
    }
}
