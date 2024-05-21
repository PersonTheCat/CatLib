package personthecat.catlib.config.fabric;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.util.LibReference;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriterOptions;

@Config(name = LibReference.MOD_ID)
public class LibConfigImpl implements ConfigData {

    @Comment("Miscellaneous settings to configure CatLib and its dependents.")
    General general = new General();

    @Comment("Configures the style of any JSON, XJS, or other data file.")
    Formatting formatting = new Formatting();

    @Comment("Configures features related to debugging / testing Catlib itself.")
    Debug debug = new Debug();

    private static final LibConfigImpl CONFIG;

    public static void register() {}

    public static boolean enableCatlibCommands() {
        return CONFIG.general.enableCatlibCommands;
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

    public static boolean enableTestError() {
        return CONFIG.debug.enableTestError;
    }

    static {
        AutoConfig.register(LibConfigImpl.class, DjsConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(LibConfigImpl.class).getConfig();
    }
    
    @Override
    public void validatePostLoad() {
        final JsonWriterOptions configured =
            new JsonWriterOptions()
                .setTabSize(this.formatting.tabSize)
                .setMinSpacing(this.formatting.minSpacing)
                .setMaxSpacing(this.formatting.maxSpacing)
                .setDefaultSpacing(this.formatting.defaultSpacing)
                .setAllowCondense(this.formatting.allowCondense)
                .setOmitRootBraces(this.formatting.omitRootBraces)
                .setOmitQuotes(this.formatting.omitQuotes)
                .setOutputComments(this.formatting.outputComments)
                .setBracesSameLine(this.formatting.bracesSameLine)
                .setSmartSpacing(this.formatting.smartSpacing);
        JsonContext.setDefaultFormatting(configured);
        JsonContext.setDefaultCommentStyle(this.formatting.commentStyle);
        JsonContext.registerAlias("mcmeta", "json");
    }

    private static class General {

        @Comment("Whether to enable this library's provided commands as subcommands of /catlib.")
        boolean enableCatlibCommands = true;

        @Comment("The minimum error level to display in the error menu. (warn, error, fatal)")
        Severity errorLevel = Severity.ERROR;

        @Comment("Whether to wrap text on the error detail page. Hit W or space to toggle in game.")
        boolean wrapText = true;

        @Comment("How many lines for the display command to render in the chat before opening a\n" +
                 "dedicated screen. Set this to 0 to always open a screen.")
        int displayLength = 35;
    }

    private static class Formatting {

        @Comment("The number of spaces representing a single tab indent.")
        int tabSize = 2;

        @Comment("The minimum number of lines between values, ignoring single-line containers.")
        int minSpacing = 1;

        @Comment("The maximum number of lines between values.")
        int maxSpacing = 3;

        @Comment("The default number of lines between values (for generated configs).")
        int defaultSpacing = 1;

        @Comment("Whether to tolerate single-line containers.")
        boolean allowCondense = true;

        @Comment("Whether to skip printing root braces for any supported format (XJS, Hjson).")
        boolean omitRootBraces = true;

        @Comment("Whether to automatically remove quotes from generated configs (XJS, Hjson).")
        boolean omitQuotes = true;

        @Comment("Whether to print comments (this comment will be deleted).")
        boolean outputComments = true;

        @Comment("Whether to open containers on the same line (Java style instead of C# style).")
        boolean bracesSameLine = true;

        @Comment("Whether to insert extra lines around commented values and containers.")
        boolean smartSpacing = true;

        @Comment("The default comment style to use for generated configs.\n" +
                 "Supported types: LINE, HASH, BLOCK, LINE_DOC, MULTILINE_DOC")
        CommentStyle commentStyle = CommentStyle.LINE;
    }

    private static class Debug {

        @Comment("Whether to add a debug error message to the error menu.")
        boolean enableTestError = false;
    }
}
