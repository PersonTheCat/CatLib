package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import personthecat.catlib.event.registry.DynamicRegistries;

import java.util.Map.Entry;
import java.util.stream.Stream;

@UtilityClass
public class FeatureSupport {

    /**
     * Determines whether the given {@link ConfiguredFeature} contains any features of
     * the given type.
     *
     * @param cf The configured feature being compared against.
     * @param f  The raw feature type which this feature may contain.
     * @return <code>true</code>, if the features match.
     */
    public static boolean matches(final ConfiguredFeature<?, ?> cf, final Feature<?> f) {
        return cf.getFeatures().anyMatch(c -> f.equals(c.feature));
    }

    /**
     * Streams every matching configured feature to its resource key.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching features and their resource keys.
     */
    public static Stream<Entry<ResourceKey<ConfiguredFeature<?, ?>>, ConfiguredFeature<?, ?>>> getMatching(final Feature<?> f) {
        return DynamicRegistries.CONFIGURED_FEATURES.streamEntries()
            .filter(c -> matches(c.getValue(), f));
    }

    /**
     * Streams the {@link ResourceLocation} of every {@link ConfiguredFeature} containing
     * the given feature.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching feature IDs.
     */
    public static Stream<ResourceLocation> getIds(final Feature<?> f) {
        return getMatching(f).map(e -> e.getKey().location());
    }

    /**
     * Streams every {@link ConfiguredFeature} containing the given feature type.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching features.
     */
    public static Stream<ConfiguredFeature<?, ?>> getFeatures(final Feature<?> f) {
        return getMatching(f).map(Entry::getValue);
    }
}
