package personthecat.catlib.event.world;

import lombok.Getter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

@Getter
public class RegistrySet {
    private final RegistryAccess registries;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<ConfiguredFeature<?, ?>> features;
    private final Registry<ConfiguredStructureFeature<?, ?>> structures;

    public RegistrySet(final RegistryAccess registries) {
        this.registries = registries;
        this.carvers = registries.registryOrThrow(Registry.CONFIGURED_CARVER_REGISTRY);
        this.features = registries.registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY);
        this.structures = registries.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
    }
}
