package personthecat.catlib.config.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.data.ModDescriptor;

import java.io.File;
import java.util.Objects;

public class ConfigEvaluatorImpl {
    static {
        DjsConfigFormat.registerFileFormat();
    }

    public static void register(ModDescriptor mod, File file, CategoryValue config) {
        final ModContainer ctx = ModLoadingContext.get().getActiveContainer();
        final String expected = mod.getModId();
        final String modId = ctx.getModId();
        if (!expected.equals(modId)) {
            throw new IllegalStateException("Attempted to register config for " + expected + " from " + modId);
        }
        final ModConfigGenerator generator = new ModConfigGenerator(mod, config);
        final ModConfigSpec spec = generator.generateSpec();
        ctx.addConfig(new ModConfig(ModConfig.Type.COMMON, spec, ctx, file.getAbsolutePath()));
        generator.fireOnConfigUpdated();

        final IEventBus modBus = Objects.requireNonNull(ctx.getEventBus(), "No event bus for mod: " + modId);
        modBus.addListener((ModConfigEvent e) -> {
            if (spec.isLoaded()) generator.updateConfig(spec);
        });
    }
}
