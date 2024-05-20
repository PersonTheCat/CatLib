package personthecat.catlib.event.world.neo;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.registry.DynamicRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class FeatureModificationHook implements BiomeModifier {
    public static final BiomeModifier INSTANCE = new FeatureModificationHook();
    public static final Codec<BiomeModifier> CODEC = Codec.unit(INSTANCE);

    private FeatureModificationHook() {}

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase, final Builder builder) {
        if (phase != Phase.REMOVE) {
            return; // must be late for now to support OSV, even for additions
        }
        final ResourceLocation id = DynamicRegistries.BIOMES.keyOf(biome);
        if (!FeatureModificationEvent.hasEvent(id)) {
            return;
        }
        final MinecraftServer server = Objects.requireNonNull(
            ServerLifecycleHooks.getCurrentServer(), "Modification hook called out of sequence");
        final RegistryAccess registries = server.registryAccess();
        final FeatureModificationContext ctx =
            new FeatureModificationContextImpl(biome.value(), id, registries, builder);

        FeatureModificationEvent.global().invoker().accept(ctx);
        FeatureModificationEvent.get(id).accept(ctx);
    }

    @Override
    public @NotNull Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
