package personthecat.catlib.util.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

@SuppressWarnings("unused")
public final class McUtilsImpl {

    private McUtilsImpl() {}

    public static File getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    public static String getPlatform() {
        return "fabric";
    }

    public static boolean isModLoaded(final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean isDedicatedServer() {
        return EnvType.SERVER == FabricLoader.getInstance().getEnvironmentType();
    }
}
