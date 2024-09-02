package personthecat.catlib.event.world.forge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.minecraftforge.server.ServerLifecycleHooks;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.registry.DynamicRegistries;

import java.util.Objects;

public class FeatureModificationHook implements BiomeModifier {
    public static final BiomeModifier INSTANCE = new FeatureModificationHook();
    public static final MapCodec<BiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private FeatureModificationHook() {}

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void modify(final Holder<Biome> biome, final Phase phase, final Builder builder) {
        if (phase != Phase.REMOVE) {
            return; // must be late for now to support OSV, even for additions
        }
        final ResourceLocation id = DynamicRegistries.BIOME.keyOf(biome);
        if (!FeatureModificationEvent.hasEvent(id)) {
            return;
        }
        final MinecraftServer server = Objects.requireNonNull(
            ServerLifecycleHooks.getCurrentServer(), "Modification hook called out of sequence");
        final RegistryAccess registries = server.registryAccess();
        final FeatureModificationContext ctx =
            new FeatureModificationContextImpl(biome, id, registries, builder);

        FeatureModificationEvent.global().invoker().accept(ctx);
        FeatureModificationEvent.get(id).accept(ctx);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
