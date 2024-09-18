package personthecat.catlib.event.world.forge;

import com.google.common.base.Stopwatch;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import personthecat.catlib.event.registry.RegistryMapSource;
import personthecat.catlib.event.world.ClientFeatureContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.mixin.forge.ModifiableBiomeInfoAccessor;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.RegistryHandle;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
public class ClientFeatureHookImpl {

    public static void modifyBiomes(RegistryAccess registries) {
        final Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        final Stopwatch sw = Stopwatch.createStarted();

        // First, inject carver / feature registries, since the delegate API will attempt
        // to load them, even though it will never use them.
        final var currentRegistries = registries.registries().map(RegistryAccess.RegistryEntry::value);
        final var unusedRegistries = Stream.of(DynamicRegistries.CONFIGURED_CARVER, DynamicRegistries.PLACED_FEATURE)
            .map(RegistryHandle::asRegistry);
        registries = new RegistryMapSource(Stream.concat(currentRegistries, unusedRegistries)).asRegistryAccess();

        // Then, sort the biome keys in a way that matches Fabric's server modification logic
        final List<ResourceKey<Biome>> keys = biomes.entrySet().stream()
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparingInt(key -> biomes.getId(biomes.getOrThrow(key))))
            .toList();

        int biomesModified = 0;

        // Finally, apply all modifications, ignoring features, carvers, and mobs
        for (final var key : keys) {
            if (FeatureModificationEvent.hasEvent(key.location())) {
                final var biome = biomes.getHolderOrThrow(key);
                final var info = biome.value().modifiableBiomeInfo();
                final var builder = BiomeInfo.Builder.copyOf(info.getOriginalBiomeInfo());
                final var ctx = new ClientFeatureContext(
                    new FeatureModificationContextImpl(biome, key.location(), registries, builder));

                FeatureModificationEvent.global().invoker().accept(ctx);
                FeatureModificationEvent.get(key.location()).accept(ctx);

                ((ModifiableBiomeInfoAccessor) info).setModifiedBiomeInfo(builder.build());
                biomesModified++;
            }
        }

        // Since we aren't modifying features or carvers, there's no need to fix anything.
        // This technically replaces client carver / feature arrays with mutable collections,
        // but the collections will never be queried on the client side
        log.info("Applied modifications {} of {} client biomes in {}", biomesModified, keys.size(), sw);
    }
}
