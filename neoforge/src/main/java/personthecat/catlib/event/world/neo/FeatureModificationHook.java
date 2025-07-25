package personthecat.catlib.event.world.neo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.registry.DynamicRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class FeatureModificationHook implements BiomeModifier {
    public static final BiomeModifier INSTANCE = new FeatureModificationHook();
    public static final MapCodec<BiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private FeatureModificationHook() {}

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase, final Builder builder) {
        final var libPhase = convertPhase(phase);
        if (libPhase == null) {
            return;
        }
        final var event = FeatureModificationEvent.get(libPhase);
        if (!event.isValidForBiome(biome)) {
            return;
        }
        final var server = Objects.requireNonNull(
            ServerLifecycleHooks.getCurrentServer(), "Modification hook called out of sequence");
        final var registries = server.registryAccess();
        final var key = DynamicRegistries.BIOME.keyOf(biome);

        event.modifyBiome(new FeatureModificationContextImpl(biome, key, registries, builder));
    }

    private static @Nullable FeatureModificationEvent.Phase convertPhase(final Phase phase) {
        return switch (phase) {
            case ADD -> FeatureModificationEvent.Phase.ADDITIONS;
            case REMOVE -> FeatureModificationEvent.Phase.REMOVALS;
            case MODIFY -> FeatureModificationEvent.Phase.MODIFICATIONS;
            case BEFORE_EVERYTHING, AFTER_EVERYTHING -> null;
        };
    }

    @Override
    public @NotNull MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
