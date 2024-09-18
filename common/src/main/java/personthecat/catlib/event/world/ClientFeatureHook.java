package personthecat.catlib.event.world;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.RegistryAccess;

public final class ClientFeatureHook {
    private ClientFeatureHook() {}

    @ExpectPlatform
    public static void modifyBiomes(final RegistryAccess registries) {}
}
