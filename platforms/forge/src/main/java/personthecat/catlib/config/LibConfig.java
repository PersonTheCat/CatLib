package personthecat.catlib.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
@InheritMissingMembers
public class LibConfig {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtils.getConfigDir() + "/" + LibReference.MOD_ID;
    private static final HjsonFileConfig COMMON_CFG = new HjsonFileConfig(FILENAME + ".hjson");

    static { COMMON.push("general"); }

    private static final BooleanValue ENABLE_LIB_COMMANDS_VALUE = COMMON
        .comment("Whether to enable this library's provided commands as regular commands.")
        .define("enableGlobalLibCommands", false);

    private static final EnumValue<Severity> ERROR_LEVEL_VALUE = COMMON
        .comment("The minimum error level to display in the error menu.")
        .defineEnum("errorLevel", Severity.ERROR);

    @Overwrite
    public static boolean enableGlobalLibCommands() {
        return ENABLE_LIB_COMMANDS_VALUE.get();
    }

    @Overwrite
    public static Severity getErrorLevel() {
        return ERROR_LEVEL_VALUE.get();
    }

    public static void register(final ModContainer ctx) {
        ctx.addConfig(new CustomModConfig(ModConfig.Type.COMMON, COMMON.build(), ctx, COMMON_CFG));
    }
}
