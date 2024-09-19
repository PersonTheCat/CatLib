package personthecat.catlib.config;

import personthecat.catlib.CatLib;
import personthecat.catlib.config.Config.Comment;
import personthecat.catlib.config.Config.Range;
import personthecat.catlib.event.error.Severity;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriterOptions;

@Config
public class LibConfig implements Config.Listener {
    private static final LibConfig INSTANCE = new LibConfig();

    @Comment("Miscellaneous settings to configure CatLib and its dependents.")
    General general = new General();

    @Comment("Settings related to command processing for CatLib and its dependents.")
    Commands commands = new Commands();

    @Comment("Configures the style of any JSON, DJS, or other data file.")
    Formatting formatting = new Formatting();

    @Comment("Configures features related to debugging / testing Catlib itself.")
    Debug debug = new Debug();

    @Override
    public void onConfigUpdated() {
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

    public static void register() {
        ConfigEvaluator.loadAndRegister(CatLib.MOD, INSTANCE);
    }

    public static boolean enableCatlibCommands() {
        return INSTANCE.commands.enableCatlibCommands;
    }

    public static Severity errorLevel() {
        if (INSTANCE == null) return Severity.WARN; // tolerate errors loading this config
        return INSTANCE.general.errorLevel;
    }

    public static boolean wrapText() {
        return INSTANCE.commands.wrapText;
    }

    public static int displayLength() {
        return INSTANCE.commands.displayLength;
    }

    public static boolean enableTestError() {
        return INSTANCE.debug.enableTestError;
    }

    public static boolean enableTestColors() {
        return INSTANCE.debug.enableTestColors;
    }

    public static int defaultUsageLineLength() {
        return INSTANCE.commands.defaultUsageLineLength;
    }

    public static int defaultUsageLineCount() {
        return INSTANCE.commands.defaultUsageLineCount;
    }

    public static int defaultUsageCommandCount() {
        return INSTANCE.commands.defaultUsageCommandCount;
    }

    static class General {

        @Comment("The minimum error level to display in the error menu.")
        Severity errorLevel = Severity.ERROR;
    }

    static class Commands {

        @Comment("Whether to enable this library's provided commands as subcommands of /catlib.")
        boolean enableCatlibCommands = true;

        @Comment("Whether to wrap text on the error detail page. Hit W or space to toggle in game.")
        boolean wrapText = true;

        @Range(min = 0)
        @Comment("How many lines for the display command to render in the chat before opening a\n" +
                "dedicated screen. Set this to 0 to always open a screen.")
        int displayLength = 35;

        @Comment("The default number of characters per line on generated help pages.")
        int defaultUsageLineLength = 60;

        @Comment("The default number of lines before any help page is forcibly wrapped.")
        int defaultUsageLineCount = 15;

        @Comment("The default preferred number of commands to display on each generated help page.")
        int defaultUsageCommandCount = 5;
    }

    static class Formatting {

        @Range(min = 0, max = 8)
        @Comment("The number of spaces representing a single tab indent.")
        int tabSize = 2;

        @Range(min = 0)
        @Comment("The minimum number of lines between values, ignoring single-line containers.")
        int minSpacing = 1;

        @Range(min = 0)
        @Comment("The maximum number of lines between values.")
        int maxSpacing = 3;

        @Range(min = 0)
        @Comment("The default number of lines between values (for generated configs).")
        int defaultSpacing = 1;

        @Comment("Whether to tolerate single-line containers.")
        boolean allowCondense = true;

        @Comment("Whether to skip printing root braces for any supported format (DJS, Hjson).")
        boolean omitRootBraces = true;

        @Comment("Whether to automatically remove quotes from generated configs (DJS, Hjson).")
        boolean omitQuotes = true;

        @Comment("Whether to print comments (this comment will be deleted).")
        boolean outputComments = true;

        @Comment("Whether to open containers on the same line (Java style instead of C# style).")
        boolean bracesSameLine = true;

        @Comment("Whether to insert extra lines around commented values and containers.")
        boolean smartSpacing = true;

        @Comment("The default comment style to use for generated configs.")
        CommentStyle commentStyle = CommentStyle.LINE;
    }

    static class Debug {

        @Comment("Whether to add a debug error message to the error menu.")
        boolean enableTestError = false;

        @Comment("Whether to modify colors in all biomes to test client-side biome modifications.")
        boolean enableTestColors = true;
    }
}
