package personthecat.catlib.util.neo;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.Environment;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.ModPlatform;
import personthecat.catlib.versioning.Version;

import java.io.File;
import java.util.Optional;

@SuppressWarnings("unused") // all method are impls
public final class McUtilsImpl {

    private McUtilsImpl() {}

    public static File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    public static ModPlatform getPlatform() {
        return ModPlatform.NEO_FORGE;
    }

    public static boolean isModLoaded(final String id) {
        return ModList.get().isLoaded(id);
    }

    public static boolean isDedicatedServer() {
        return Dist.DEDICATED_SERVER == Environment.get().getDist();
    }

    public static Optional<ModDescriptor> getMod(final String id) {
        return ModList.get().getModContainerById(id).map(McUtilsImpl::toDescriptor);
    }

    private static ModDescriptor toDescriptor(final ModContainer container) {
        final var info = container.getModInfo();
        return ModDescriptor.builder()
            .modId(info.getModId())
            .name(info.getDisplayName())
            .version(Version.tryParse(info.getVersion().toString()).orElse(Version.ZERO))
            .build();
    }
}
