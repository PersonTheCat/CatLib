package personthecat.catlib.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.forge.McUtilsImpl;
import xjs.comments.CommentStyle;
import xjs.serialization.JsonContext;
import xjs.serialization.writer.JsonWriterOptions;

public class LibConfigImpl {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtilsImpl.getConfigDir() + "/" + LibReference.MOD_ID;
    private static final XjsFileConfig COMMON_CFG = new XjsFileConfig(FILENAME + ".xjs");

    private static final BooleanValue ENABLE_LIB_COMMANDS_VALUE = COMMON
        .comment("Whether to enable this library's provided commands as regular commands.")
        .define("general.enableGlobalLibCommands", false);

    private static final EnumValue<Severity> ERROR_LEVEL_VALUE = COMMON
        .comment("The minimum error level to display in the error menu.")
        .defineEnum("general.errorLevel", Severity.ERROR);

    private static final BooleanValue WRAP_TEXT_VALUE = COMMON
        .comment("Whether to wrap text on the error detail page. Hit W or space to toggle in game.")
        .define("general.wrapText", true);

    private static final IntValue DISPLAY_LENGTH_VALUE = COMMON
        .comment("How many lines for the display command to render in the chat before opening a",
                 "dedicated screen. Set this to 0 to always open a screen.")
        .defineInRange("general.displayLength", 35, 0, 100);

    private static final IntValue TAB_SIZE_VALUE = COMMON
        .comment("The number of spaces representing a single tab indent.")
        .defineInRange("formatting.tabSize", 2, 0, Integer.MAX_VALUE);

    private static final IntValue MIN_SPACING_VALUE = COMMON
        .comment("The minimum number of lines between values, ignoring single-line containers.")
        .defineInRange("formatting.minSpacing", 1, 0, Integer.MAX_VALUE);

    private static final IntValue MAX_SPACING_VALUE = COMMON
        .comment("The maximum number of lines between values.")
        .defineInRange("formatting.maxSpacing", 3, 0, Integer.MAX_VALUE);

    private static final IntValue DEFAULT_SPACING_VALUE = COMMON
        .comment("The default number of lines between values (for generated configs.")
        .defineInRange("formatting.defaultSpacing", 1, 0, Integer.MAX_VALUE);

    private static final BooleanValue ALLOW_CONDENSE_VALUE = COMMON
        .comment("Whether to tolerate single-line containers")
        .define("formatting.allowCondense", true);

    private static final BooleanValue OMIT_ROOT_BRACES_VALUE = COMMON
        .comment("Whether to skip printing root braces for any supported format (XJS, Hjson).")
        .define("formatting.omitRootBraces", true);

    private static final BooleanValue OMIT_QUOTES_VALUE = COMMON
        .comment("Whether to automatically remove quotes from generated configs (XJS, Hjson).")
        .define("formatting.omitQuotes", true);

    private static final BooleanValue OUTPUT_COMMENTS_VALUE = COMMON
        .comment("Whether to print comments (this comment will be deleted).")
        .define("formatting.outputComments", true);

    private static final BooleanValue BRACES_SAME_LINE_VALUE = COMMON
        .comment("Whether to open containers on the same line (Java style instead of C# style).")
        .define("formatting.bracesSameLine", true);

    private static final BooleanValue SMART_SPACING_VALUE = COMMON
        .comment("Whether to insert extra lines around commented values and containers.")
        .define("formatting.smartSpacing", true);

    private static final EnumValue<CommentStyle> COMMENT_STYLE_VALUE = COMMON
        .comment("The default comment style to use for generated configs.")
        .defineEnum("formatting.commentStyle", CommentStyle.LINE);

    private static final ForgeConfigSpec COMMON_SPEC = COMMON.build();

    public static boolean enableGlobalLibCommands() {
        return ENABLE_LIB_COMMANDS_VALUE.get();
    }

    public static Severity errorLevel() {
        return ERROR_LEVEL_VALUE.get();
    }

    public static boolean wrapText() {
        return WRAP_TEXT_VALUE.get();
    }

    public static int displayLength() {
        return DISPLAY_LENGTH_VALUE.get();
    }

    public static void register() {
        final ModContainer ctx = ModLoadingContext.get().getActiveContainer();
        JsonContext.setDefaultFormatting(getFormatting());
        JsonContext.setDefaultCommentStyle(COMMENT_STYLE_VALUE.get());
        JsonContext.registerAlias("mcmeta", "json");
        ctx.addConfig(new CustomModConfig(ModConfig.Type.COMMON, COMMON_SPEC, ctx, COMMON_CFG));
    }

    private static JsonWriterOptions getFormatting() {
        return new JsonWriterOptions()
            .setTabSize(TAB_SIZE_VALUE.get())
            .setMinSpacing(MIN_SPACING_VALUE.get())
            .setMaxSpacing(MAX_SPACING_VALUE.get())
            .setDefaultSpacing(DEFAULT_SPACING_VALUE.get())
            .setAllowCondense(ALLOW_CONDENSE_VALUE.get())
            .setOmitRootBraces(OMIT_ROOT_BRACES_VALUE.get())
            .setOmitQuotes(OMIT_QUOTES_VALUE.get())
            .setOutputComments(OUTPUT_COMMENTS_VALUE.get())
            .setBracesSameLine(BRACES_SAME_LINE_VALUE.get())
            .setSmartSpacing(SMART_SPACING_VALUE.get());
    }
}
