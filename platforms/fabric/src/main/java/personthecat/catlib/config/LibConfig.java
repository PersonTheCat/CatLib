package personthecat.catlib.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.util.LibReference;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.function.Supplier;

@OverwriteClass
@Config(name = LibReference.MOD_ID)
public class LibConfig implements ConfigData {

    boolean test = false;

    @Excluded
    private static final Lazy<LibConfig> CONFIG =
        Lazy.of(() -> AutoConfig.getConfigHolder(LibConfig.class).getConfig());

    @Excluded
    @Overwrite
    public static final Supplier<Boolean> enableGlobalLibCommands = () -> true;
}
