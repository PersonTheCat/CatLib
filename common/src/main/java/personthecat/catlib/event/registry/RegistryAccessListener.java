package personthecat.catlib.event.registry;

import net.minecraft.core.RegistryAccess;

public interface RegistryAccessListener {

    void onRegistryAccess(RegistryAccess registries);

    default void startListening() {
        RegistryAccessEvent.EVENT.register(this::onRegistryAccess);
    }
}
