package personthecat.catlib.config.fabric;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.config.XjsConfigSerializer;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.util.LibReference;

@Config(name = LibReference.MOD_ID)
public class LibConfigImpl implements ConfigData {

    General general = new General();

    private static final LibConfigImpl CONFIG;

    public static void register() {}

    public static boolean enableGlobalLibCommands() {
        return CONFIG.general.enableGlobalLibCommands;
    }

    public static Severity errorLevel() {
        return CONFIG.general.errorLevel;
    }

    public static boolean wrapText() {
        return CONFIG.general.wrapText;
    }

    public static int displayLength() {
        return CONFIG.general.displayLength;
    }

    static {
        AutoConfig.register(LibConfigImpl.class, XjsConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(LibConfigImpl.class).getConfig();
    }

    private static class General {

        @Comment("Whether to enable this library's provided commands as regular commands.")
        boolean enableGlobalLibCommands = false;

        @Comment("The minimum error level to display in the error menu. (warn, error, fatal)")
        Severity errorLevel = Severity.ERROR;

        @Comment("Whether to wrap text on the error detail page. Hit W or space to toggle in game.")
        boolean wrapText = true;

        @Comment("How many lines for the display command to render in the chat before opening a\n" +
                 "dedicated screen. Set this to 0 to always open a screen.")
        int displayLength = 35;
    }
}
