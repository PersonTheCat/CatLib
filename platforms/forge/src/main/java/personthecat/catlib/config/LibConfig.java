package personthecat.catlib.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.function.Supplier;

@OverwriteClass
public class LibConfig {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtils.getConfigDir() + "/" + LibReference.MOD_ID;
    private static final HjsonFileConfig COMMON_CFG = new HjsonFileConfig(FILENAME + "-common.hjson");

    static { COMMON.push("general"); }

    private static final BooleanValue ENABLE_LIB_COMMANDS_VALUE = COMMON
        .comment("Whether to enable this library's provided commands as regular commands.")
        .define("enableGlobalLibCommands", false);

    @Overwrite
    public static final Supplier<Boolean> ENABLE_GLOBAL_LIB_COMMANDS = ENABLE_LIB_COMMANDS_VALUE::get;

    public static void register(final ModContainer ctx) {
        ctx.addConfig(new CustomModConfig(ModConfig.Type.COMMON, COMMON.build(), ctx, COMMON_CFG));
    }
}
