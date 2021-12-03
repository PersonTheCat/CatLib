package personthecat.catlib.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.util.LibReference;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
@InheritMissingMembers
@Config(name = LibReference.MOD_ID)
public class LibConfig implements ConfigData {

    General general = new General();

    private static final Lazy<LibConfig> CONFIG =
        Lazy.of(() -> AutoConfig.getConfigHolder(LibConfig.class).getConfig());

    @Overwrite
    public static boolean enableGlobalLibCommands() {
        return CONFIG.get().general.enableGlobalLibCommands;
    }

    @Overwrite
    public static Severity getErrorLevel() {
        return CONFIG.get().general.errorLevel;
    }

    @Overwrite
    public static boolean wrapText() {
        return CONFIG.get().general.wrapText;
    }

    private static class General {

        @Comment("Whether to enable this library's provided commands as regular commands.")
        boolean enableGlobalLibCommands = false;

        @Comment("The minimum error level to display in the error menu. (warn, error, fatal)")
        Severity errorLevel = Severity.ERROR;

        @Comment("Whether to wrap text on the error detail page. Hit W or space to toggle in game.")
        boolean wrapText = true;
    }
}
