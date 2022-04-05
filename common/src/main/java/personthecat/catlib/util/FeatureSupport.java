package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import personthecat.catlib.registry.DynamicRegistries;

import java.util.Map.Entry;
import java.util.stream.Stream;

@UtilityClass
public class FeatureSupport {

    /**
     * Determines whether the given {@link PlacedFeature} contains any features of the
     * given type.
     *
     * @param pf The placed feature being compared against.
     * @param f  The raw feature type which this feature may contain.
     * @return <code>true</code>, if the features match.
     */
    public static boolean matches(final PlacedFeature pf, final Feature<?> f) {
        return pf.getFeatures().anyMatch(c -> f.equals(c.feature()));
    }

    /**
     * Streams every matching configured feature to its resource key.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching features and their resource keys.
     */
    public static Stream<Entry<ResourceKey<PlacedFeature>, PlacedFeature>> getMatching(final Feature<?> f) {
        return DynamicRegistries.PLACED_FEATURES.streamEntries()
            .filter(c -> matches(c.getValue(), f));
    }

    /**
     * Streams the {@link ResourceLocation} of every {@link PlacedFeature} containing
     * the given feature.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching feature IDs.
     */
    public static Stream<ResourceLocation> getIds(final Feature<?> f) {
        return getMatching(f).map(e -> e.getKey().location());
    }

    /**
     * Streams every {@link PlacedFeature} containing the given feature type.
     *
     * @param f The raw feature type being queried against.
     * @return A stream of all matching features.
     */
    public static Stream<PlacedFeature> getFeatures(final Feature<?> f) {
        return getMatching(f).map(Entry::getValue);
    }
}
