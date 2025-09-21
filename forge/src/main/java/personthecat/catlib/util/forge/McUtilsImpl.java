package personthecat.catlib.util.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.Environment;

import java.nio.files.Path;

public class McUtilsImpl {

    private McUtilsImpl() {}

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getPlatform() {
        return "forge";
    }

    public static boolean isModLoaded(final String id) {
        return ModList.get().isLoaded(id);
    }

    public static boolean isDedicatedServer() {
        return Dist.DEDICATED_SERVER == Environment.get().getDist();
    }
}
