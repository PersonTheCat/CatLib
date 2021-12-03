package personthecat.catlib;

import lombok.extern.log4j.Log4j2;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.DefaultLibCommands;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.HjsonConfigSerializer;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.CheckErrorsEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.RegistrySet;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.exception.LinterTestException;
import personthecat.catlib.mixin.BiomeModificationContextAccessor;
import personthecat.catlib.util.LibReference;

import java.util.Arrays;
import java.util.List;

@Log4j2
public class CatLib implements ModInitializer {

    private static final ModDescriptor OSV = ModDescriptor.builder().modId("osv").name("Ore Stone Variants").build();
    private static final ModDescriptor CG = ModDescriptor.builder().modId("cavegenerator").name("Cave Generator").build();

    private static final List<FormattedException> TEST_ERRORS = Arrays.asList(
        new LinterTestException(),
        new GenericFormattedException(new RuntimeException("Test 1")),
        new GenericFormattedException(new RuntimeException("Test 2")),
        new GenericFormattedException(new RuntimeException("Test 3"), "#3 has a tooltip! Look at me! Look at me!"),
        new GenericFormattedException(new RuntimeException("Test 4")),
        new GenericFormattedException(new RuntimeException("Test 5")),
        new GenericFormattedException(new RuntimeException("Test 6")),
        new GenericFormattedException(new RuntimeException("Test 7")),
        new GenericFormattedException(new RuntimeException("Test 8")),
        new GenericFormattedException(new RuntimeException("Test 9"), "#9 has a long tooltip! Let's put a lot of text in there and see what happens. I'll start by adding second and third sentence. Here you go."),
        new GenericFormattedException(new RuntimeException("Test 10")),
        new GenericFormattedException(new RuntimeException("Test 11"), "#11 has a tooltip with\nmultiple lines!"),
        new GenericFormattedException(new RuntimeException("Test 12")),
        new GenericFormattedException(new RuntimeException("Test 13")),
        new GenericFormattedException(new RuntimeException("Test 14")),
        new GenericFormattedException(new RuntimeException("Test 15"))
    );

    private static final List<FormattedException> TEST_ERRORS_2 = Arrays.asList(
        new GenericFormattedException(new RuntimeException("Test 20")),
        new GenericFormattedException(new RuntimeException("Test 21")),
        new GenericFormattedException(new RuntimeException("Test 22")),
        new GenericFormattedException(new RuntimeException("Test 23")),
        new GenericFormattedException(new RuntimeException("Test 24")),
        new GenericFormattedException(new RuntimeException("Test 25"))
    );

    @Override
    public void onInitialize() {
        AutoConfig.register(LibConfig.class, HjsonConfigSerializer::new);

        TEST_ERRORS.forEach(e -> LibErrorContext.registerSingle(OSV, e));
        TEST_ERRORS_2.forEach(e -> LibErrorContext.registerSingle(OSV, e));
        TEST_ERRORS_2.forEach(e -> LibErrorContext.registerSingle(CG, e));

        CheckErrorsEvent.EVENT.register(() ->
            log.info("Checking for errors..."));

        EnumArgument.register();
        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();
        RegistryArgument.register();

        this.setupBiomeModificationHook();

        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
        });

        if (LibConfig.enableGlobalLibCommands()) {
            CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR)
                .addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true))
                .registerAll();
        }

        ServerWorldEvents.LOAD.register((s, l) -> CommonWorldEvent.LOAD.invoker().accept(l));
        ServerWorldEvents.UNLOAD.register((s, l) -> CommonWorldEvent.UNLOAD.invoker().accept(l));
        ServerPlayConnectionEvents.JOIN.register((h, tx, s) -> CommonPlayerEvent.LOGIN.invoker().accept(h.player, s));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> CommonPlayerEvent.LOGOUT.invoker().accept(h.player, s));
    }

    @SuppressWarnings("deprecation")
    private void setupBiomeModificationHook() {
        BiomeModifications.create(new ResourceLocation(LibReference.MOD_ID, "biome_updates"))
            .add(ModificationPhase.REMOVALS, s -> true, (s, m) -> {
                final RegistrySet registries = new RegistrySet(((BiomeModificationContextAccessor) m).getRegistries());
                FeatureModificationEvent.EVENT.invoker().accept(new FeatureModificationContext(s, registries));
            });
    }
}
