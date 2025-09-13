package personthecat.catlib;

import com.mojang.brigadier.arguments.ArgumentType;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.util.CommonColors;
import personthecat.catlib.command.CatLibCommands;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.arguments.EnumArgument;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.JsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.util.LibUtil;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.versioning.Version;
import personthecat.catlib.versioning.VersionTracker;

@Log4j2
public abstract class CatLib {
    public static final String ID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String RAW_VERSION = "@MOD_VERSION@";
    public static final Version VERSION = Version.parse(RAW_VERSION);
    public static final ModDescriptor MOD =
        ModDescriptor.builder().modId(ID).name(NAME).version(VERSION).configFolder(McUtils.getConfigDir()).buildAndRegister();
    public static final VersionTracker VERSION_TRACKER = VersionTracker.trackModVersion(MOD);

    protected final void init() {
        LibConfig.register();
        this.registerArgumentTypes();
    }

    protected final void commonSetup() {
        RegistryAccessEvent.EVENT.register(RegistryAddedEvent::onRegistryAccess);
        DataRegistryEvent.PRE.register(source ->
            DynamicRegistries.updateRegistries(source.asRegistryAccess()));
        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(MOD)
            .addCommand(CatLibCommands.ERROR_MENU);
        if (LibConfig.enableCatlibCommands()) {
            ctx.addLibCommands();
        }
        ctx.registerAll();
        GameReadyEvent.COMMON.register(() -> {
            if (VERSION_TRACKER.isUpgraded()) {
                log.info("Upgrade detected. Welcome to CatLib {}", VERSION);
            }
        });
        CommonWorldEvent.LOAD.register(a -> {
            if (McUtils.isDedicatedServer()) {
                LibErrorContext.outputServerErrors(false);
            }
        });
        CommonPlayerEvent.LOGIN.register((e, s) -> {
            if (McUtils.isClientSide() && LibErrorContext.hasErrors()) {
                LibErrorContext.broadcastErrors(e);
            }
        });
        enableDebugFeatures();
    }

    private void registerArgumentTypes() {
        this.registerArgumentType(
            "enum_argument", LibUtil.asParentType(EnumArgument.class),
            EnumArgument.INFO);
        this.registerArgumentType(
            "file_argument", FileArgument.class,
            FileArgument.INFO);
        this.registerArgumentType(
            "json_argument", JsonArgument.class,
            JsonArgument.INFO);
        this.registerArgumentType(
            "path_argument", PathArgument.class,
            SingletonArgumentInfo.contextFree(PathArgument::new));
        this.registerArgumentType(
            "registry_argument", LibUtil.asParentType(RegistryArgument.class),
            RegistryArgument.INFO);
    }

    protected abstract <T extends ArgumentType<?>> void registerArgumentType(
        final String id, final Class<T> clazz, final ArgumentTypeInfo<T, ?> info);

    protected final void shutdown() {
        DynamicRegistries.onSeverClosed();
    }

    private void enableDebugFeatures() {
        if (LibConfig.enableTestError()) {
            LibErrorContext.error(MOD,
                new GenericFormattedException(new RuntimeException("test error"), "tooltip working!"));
            LibErrorContext.warn(ModDescriptor.builder().modId("test").name("Test Mod").build(),
                new GenericFormattedException(new RuntimeException("test error 2"), "tooltip working... 2!"));
        }
        if (LibConfig.enableTestColors()) {
            FeatureModificationEvent.register(ctx -> {
                ctx.setSkyColor(CommonColors.BLACK);
                ctx.setWaterColor(CommonColors.RED);
                ctx.setFoliageColorOverride(CommonColors.WHITE);
            });
        }
    }
}
