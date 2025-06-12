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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        final var id = DynamicRegistries.BIOME.keyOf(biome);

        event.modifyBiome(new FeatureModificationContextImpl(biome, id, registries, builder));
    }

    private static @Nullable FeatureModificationEvent2.Phase convertPhase(final Phase phase) {
        return switch (phase) {
            case ADD -> FeatureModificationEvent2.Phase.ADDITIONS;
            case REMOVE -> FeatureModificationEvent2.Phase.REMOVALS;
            case MODIFY -> FeatureModificationEvent2.Phase.MODIFICATIONS;
            case BEFORE_EVERYTHING, AFTER_EVERYTHING -> null;
        };
    }

    @Override
    public @NotNull MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
