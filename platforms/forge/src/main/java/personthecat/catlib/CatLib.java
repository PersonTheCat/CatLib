package personthecat.catlib;

import lombok.experimental.FieldNameConstants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.util.Reference;

@Mod(Reference.MOD_ID)
@FieldNameConstants
public class CatLib {

    private final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    private final IEventBus eventBus = MinecraftForge.EVENT_BUS;

    public CatLib() {
        // jar files
        // cfg registry
        this.setupEventHandlers();
    }

    private void setupEventHandlers() {
        this.modBus.addListener(this::initCommon);
        this.eventBus.addListener(this::initServer);
    }

    @SuppressWarnings("unused")
    private void initCommon(final FMLCommonSetupEvent event) {
        // argument types
    }

    @SuppressWarnings("unused")
    private void initServer(final FMLServerStartingEvent event) {
        // commands
    }
}
