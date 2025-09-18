package personthecat.catlib.util.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.ModPlatform;
import personthecat.catlib.versioning.Version;

import java.io.File;
import java.util.Optional;

public final class McUtilsImpl {

    private McUtilsImpl() {}

    public static File getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    public static ModPlatform getPlatform() {
        return ModPlatform.FABRIC;
    }

    public static boolean isModLoaded(final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean isDedicatedServer() {
        return EnvType.SERVER == FabricLoader.getInstance().getEnvironmentType();
    }

    public static Optional<ModDescriptor> getMod(final String id) {
        return FabricLoader.getInstance().getModContainer(id).map(McUtilsImpl::toDescriptor);
    }

    private static ModDescriptor toDescriptor(final ModContainer container) {
        final var meta = container.getMetadata();
        return ModDescriptor.builder()
            .modId(meta.getId())
            .name(meta.getName())
            .version(Version.tryParse(meta.getVersion().getFriendlyString()).orElse(Version.ZERO))
            .build();
    }
}
