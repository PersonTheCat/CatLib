package personthecat.catlib.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import personthecat.catlib.CatLib;
import personthecat.catlib.util.SyncTracker;

import java.util.Map;
import java.util.Optional;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> implements SyncTracker<T> {
    @Unique
    private static final RegistrationInfo CATLIB_MODIFIED = new RegistrationInfo(
        Optional.of(new KnownPack(CatLib.ID, "synchronized", CatLib.RAW_VERSION)), Lifecycle.experimental());

    @Shadow
    private @Final Map<ResourceKey<T>, RegistrationInfo> registrationInfos;

    @Override
    public void markUpdated(final ResourceKey<T> key) {
        this.registrationInfos.put(key, CATLIB_MODIFIED);
    }
}
