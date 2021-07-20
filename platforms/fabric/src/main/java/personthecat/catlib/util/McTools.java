package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.io.File;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("unused")
public class McTools {

    @Overwrite
    public static File getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    @Overwrite
    public static boolean isModLoaded(final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
