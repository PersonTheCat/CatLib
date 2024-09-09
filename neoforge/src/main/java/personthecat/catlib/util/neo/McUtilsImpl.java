package personthecat.catlib.util.neo;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.Environment;

import java.io.File;

@SuppressWarnings("unused")
public final class McUtilsImpl {

    private McUtilsImpl() {}

    public static File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    public static String getPlatform() {
        return "neoforge";
    }

    public static boolean isModLoaded(final String id) {
        return ModList.get().isLoaded(id);
    }

    public static boolean isDedicatedServer() {
        return Dist.DEDICATED_SERVER == Environment.get().getDist();
    }
}
