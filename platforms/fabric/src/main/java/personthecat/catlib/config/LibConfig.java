package personthecat.catlib.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.core.Direction;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.util.LibReference;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@OverwriteClass
@Config(name = LibReference.MOD_ID)
public class LibConfig implements ConfigData {

    General general = new General();

    private static final Lazy<LibConfig> CONFIG =
        Lazy.of(() -> AutoConfig.getConfigHolder(LibConfig.class).getConfig());

    @Overwrite
    public static final Supplier<Boolean> ENABLE_GLOBAL_LIB_COMMANDS =
        () -> CONFIG.get().general.enableGlobalLibCommands;

    private static class General {
        @Comment("Whether to enable this library's provided commands as regular commands.")
        boolean enableGlobalLibCommands = false;
    }
}
