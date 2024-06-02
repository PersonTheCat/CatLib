package personthecat.catlib.config.fabric;

import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.McUtils;

import java.io.File;

public class ConfigEvaluatorImpl {
    public static void register(ModDescriptor mod, File file, CategoryValue config) {
        final ClothConfigGenerator generator = new ClothConfigGenerator(mod, file, config);
        generator.loadConfig();

        if (McUtils.isClientSide() && McUtils.isModLoaded("modmenu")) {
            AutoModMenuCompat.registerScreen(mod.getModId(), generator::createScreen);
        }
    }
}
