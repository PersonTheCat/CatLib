package personthecat.catlib.event.world.fabric;

import com.google.common.base.Stopwatch;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationContextImpl;
import net.fabricmc.fabric.impl.biome.modification.BiomeSelectionContextImpl;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import personthecat.catlib.event.registry.RegistryMapSource;
import personthecat.catlib.event.world.ClientFeatureContext;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
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
                final var ctx = createContext(registries, key, biomes.getOrThrow(key));
                FeatureModificationEvent.global().invoker().accept(ctx);
                FeatureModificationEvent.get(key.location()).accept(ctx);
                biomesModified++;
            }
        }

        // Since we aren't modifying features or carvers, there's no need to fix anything.
        // This technically replaces client carver / feature arrays with mutable collections,
        // but the collections will never be queried on the client side
        log.info("Applied modifications {} of {} client biomes in {}", biomesModified, keys.size(), sw);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static FeatureModificationContext createContext(
            final RegistryAccess registries, final ResourceKey<Biome> key, final Biome biome) {
        final var selectionContext =
            new BiomeSelectionContextImpl(registries, key, biome);
        final var modificationContext =
            new BiomeModificationContextImpl(registries, biome);
        return new ClientFeatureContext(
            new FeatureModificationContextImpl(selectionContext, modificationContext));
    }
}
