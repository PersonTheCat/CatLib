package personthecat.catlib.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.data.ModDescriptor;

import java.io.File;
import java.util.Objects;

public class ConfigEvaluatorImpl {
    static {
        DjsConfigFormat.registerFileFormat();
    }

    public static void register(ModDescriptor mod, File file, CategoryValue config) {
        final ModContainer container = ModLoadingContext.get().getActiveContainer();
        final String expected = mod.modId();
        final String modId = container.getModId();
        if (!expected.equals(modId)) {
            throw new IllegalStateException("Attempted to register config for " + expected + " from " + modId);
        }
        final ModConfigGenerator generator = new ModConfigGenerator(mod, config);
        final ForgeConfigSpec spec = generator.generateSpec();
        container.addConfig(new ModConfig(ModConfig.Type.COMMON, spec, container, file.getAbsolutePath()));
        generator.fireOnConfigUpdated();

        final FMLJavaModLoadingContext ctx = FMLJavaModLoadingContext.get();
        final IEventBus modBus = Objects.requireNonNull(ctx.getModEventBus(), "No event bus for mod: " + modId);
        modBus.addListener((ModConfigEvent e) -> {
            if (spec.isLoaded()) generator.updateConfig(spec);
        });
    }
}
